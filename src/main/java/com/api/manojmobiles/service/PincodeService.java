package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.pincode.*;
import com.api.manojmobiles.entity.City;
import com.api.manojmobiles.entity.ServiceablePincode;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CityRepository;
import com.api.manojmobiles.repository.ServiceablePincodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PincodeService {

    private final ServiceablePincodeRepository pincodeRepository;
    private final CityRepository cityRepository;

    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    @Transactional
    public PincodeResponseDTO createPincode(CreatePincodeRequestDTO request) {
        if (pincodeRepository.existsByPincode(request.getPincode())) {
            throw new BadRequestException("Pincode '" + request.getPincode() + "' is already registered as serviceable");
        }

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + request.getCityId()));

        if (Boolean.FALSE.equals(city.getIsActive())) {
            throw new BadRequestException("Cannot add pincode to an inactive city");
        }

        ServiceablePincode pincode = ServiceablePincode.builder()
                .pincode(request.getPincode())
                .city(city)
                .estimatedDeliveryDays(request.getEstimatedDeliveryDays())
                .codAvailable(request.getCodAvailable() != null ? request.getCodAvailable() : true)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        pincode = pincodeRepository.save(pincode);
        return mapToDTO(pincode);
    }

    @Transactional(readOnly = true)
    public List<PincodeResponseDTO> getAllPincodes() {
        return pincodeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PincodeResponseDTO> getAllPincodes(UUID cityId, String search, Pageable pageable) {
        return pincodeRepository.searchPincodes(cityId, search, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<PincodeResponseDTO> getPincodesByCity(UUID cityId) {
        if (!cityRepository.existsById(cityId)) {
            throw new ResourceNotFoundException("City not found with id: " + cityId);
        }
        return pincodeRepository.findByCityId(cityId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PincodeResponseDTO getPincodeById(UUID id) {
        ServiceablePincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode not found with id: " + id));
        return mapToDTO(pincode);
    }

    @Transactional
    public PincodeResponseDTO updatePincode(UUID id, UpdatePincodeRequestDTO request) {
        ServiceablePincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode not found with id: " + id));

        if (request.getPincode() != null && !request.getPincode().trim().isEmpty()) {
            String newPincode = request.getPincode().trim();
            if (!newPincode.equals(pincode.getPincode())) {
                if (pincodeRepository.existsByPincode(newPincode)) {
                    throw new BadRequestException("Pincode '" + newPincode + "' is already registered as serviceable");
                }
                pincode.setPincode(newPincode);
            }
        }

        if (request.getCityId() != null && (pincode.getCity() == null || !request.getCityId().equals(pincode.getCity().getId()))) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + request.getCityId()));
            if (Boolean.FALSE.equals(city.getIsActive())) {
                throw new BadRequestException("Cannot assign pincode to an inactive city");
            }
            pincode.setCity(city);
        }

        if (request.getEstimatedDeliveryDays() != null) {
            pincode.setEstimatedDeliveryDays(request.getEstimatedDeliveryDays());
        }
        if (request.getCodAvailable() != null) {
            pincode.setCodAvailable(request.getCodAvailable());
        }
        if (request.getIsActive() != null) {
            pincode.setIsActive(request.getIsActive());
        }

        pincode = pincodeRepository.save(pincode);
        return mapToDTO(pincode);
    }

    @Transactional
    public PincodeResponseDTO togglePincodeStatus(UUID id) {
        ServiceablePincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode not found with id: " + id));
        pincode.setIsActive(!Boolean.TRUE.equals(pincode.getIsActive()));
        pincode = pincodeRepository.save(pincode);
        return mapToDTO(pincode);
    }

    @Transactional
    public void deletePincode(UUID id) {
        ServiceablePincode pincode = pincodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode not found with id: " + id));
        pincodeRepository.delete(pincode);
    }

    @Transactional(readOnly = true)
    public PincodeCheckResponseDTO checkPincodeServiceability(String pincodeStr) {
        return pincodeRepository.findByPincode(pincodeStr)
                .map(pincode -> {
                    boolean isActiveCity = pincode.getCity() != null && Boolean.TRUE.equals(pincode.getCity().getIsActive());
                    boolean isActivePincode = Boolean.TRUE.equals(pincode.getIsActive());
                    boolean isServiceable = isActiveCity && isActivePincode;
                    return PincodeCheckResponseDTO.builder()
                            .pincode(pincode.getPincode())
                            .isServiceable(isServiceable)
                            .cityName(pincode.getCity() != null ? pincode.getCity().getName() : null)
                            .state(pincode.getCity() != null ? pincode.getCity().getState() : null)
                            .estimatedDeliveryDays(isServiceable ? pincode.getEstimatedDeliveryDays() : null)
                            .codAvailable(isServiceable ? pincode.getCodAvailable() : null)
                            .build();
                })
                .orElseGet(() -> PincodeCheckResponseDTO.builder()
                        .pincode(pincodeStr)
                        .isServiceable(false)
                        .build());
    }

    @Transactional
    public BulkPincodeResponseDTO bulkUploadPincodes(BulkPincodeUploadRequestDTO request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + request.getCityId()));

        if (Boolean.FALSE.equals(city.getIsActive())) {
            throw new BadRequestException("Cannot add pincodes to an inactive city");
        }

        List<ServiceablePincode> toSave = new ArrayList<>();
        List<String> addedPincodes = new ArrayList<>();
        List<String> skippedPincodes = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        Set<String> processedInBatch = new HashSet<>();

        // 1. Simple list of pincode strings
        if (request.getPincodes() != null) {
            for (String rawPin : request.getPincodes()) {
                if (rawPin == null || rawPin.trim().isEmpty()) {
                    continue;
                }
                processPincodeItem(rawPin.trim(), city, request.getEstimatedDeliveryDays(),
                        request.getCodAvailable(), request.getIsActive(),
                        toSave, addedPincodes, skippedPincodes, failureReasons, processedInBatch);
            }
        }

        // 2. Granular DTO items
        if (request.getItems() != null) {
            for (CreatePincodeRequestDTO item : request.getItems()) {
                if (item == null || item.getPincode() == null || item.getPincode().trim().isEmpty()) {
                    continue;
                }
                Integer deliveryDays = item.getEstimatedDeliveryDays() != null ? item.getEstimatedDeliveryDays() : request.getEstimatedDeliveryDays();
                Boolean cod = item.getCodAvailable() != null ? item.getCodAvailable() : request.getCodAvailable();
                Boolean active = item.getIsActive() != null ? item.getIsActive() : request.getIsActive();

                processPincodeItem(item.getPincode().trim(), city, deliveryDays, cod, active,
                        toSave, addedPincodes, skippedPincodes, failureReasons, processedInBatch);
            }
        }

        if (!toSave.isEmpty()) {
            pincodeRepository.saveAll(toSave);
        }

        return BulkPincodeResponseDTO.builder()
                .totalProcessed(addedPincodes.size() + skippedPincodes.size() + failureReasons.size())
                .addedCount(addedPincodes.size())
                .skippedCount(skippedPincodes.size())
                .failedCount(failureReasons.size())
                .addedPincodes(addedPincodes)
                .skippedPincodes(skippedPincodes)
                .failureReasons(failureReasons)
                .build();
    }

    @Transactional
    public BulkPincodeResponseDTO bulkUploadPincodesFromCsv(MultipartFile file, UUID cityId, Integer defaultDeliveryDays, Boolean defaultCodAvailable) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));

        if (Boolean.FALSE.equals(city.getIsActive())) {
            throw new BadRequestException("Cannot add pincodes to an inactive city");
        }

        List<ServiceablePincode> toSave = new ArrayList<>();
        List<String> addedPincodes = new ArrayList<>();
        List<String> skippedPincodes = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        Set<String> processedInBatch = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split("[,;\\t]+");
                String pin = tokens[0].trim().replaceAll("^\"|\"$", "");

                // Skip header if first line is not a 6-digit number
                if (lineNumber == 1 && !PINCODE_PATTERN.matcher(pin).matches()) {
                    continue;
                }

                Integer deliveryDays = defaultDeliveryDays != null ? defaultDeliveryDays : 3;
                Boolean cod = defaultCodAvailable != null ? defaultCodAvailable : true;

                if (tokens.length > 1) {
                    try {
                        deliveryDays = Integer.parseInt(tokens[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
                if (tokens.length > 2) {
                    cod = Boolean.parseBoolean(tokens[2].trim());
                }

                processPincodeItem(pin, city, deliveryDays, cod, true,
                        toSave, addedPincodes, skippedPincodes, failureReasons, processedInBatch);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CSV file for pincodes", e);
            throw new BadRequestException("Error reading CSV file: " + e.getMessage());
        }

        if (!toSave.isEmpty()) {
            pincodeRepository.saveAll(toSave);
        }

        return BulkPincodeResponseDTO.builder()
                .totalProcessed(addedPincodes.size() + skippedPincodes.size() + failureReasons.size())
                .addedCount(addedPincodes.size())
                .skippedCount(skippedPincodes.size())
                .failedCount(failureReasons.size())
                .addedPincodes(addedPincodes)
                .skippedPincodes(skippedPincodes)
                .failureReasons(failureReasons)
                .build();
    }

    private void processPincodeItem(String pin, City city, Integer deliveryDays, Boolean cod, Boolean active,
                                    List<ServiceablePincode> toSave,
                                    List<String> addedPincodes,
                                    List<String> skippedPincodes,
                                    List<String> failureReasons,
                                    Set<String> processedInBatch) {
        if (!PINCODE_PATTERN.matcher(pin).matches()) {
            failureReasons.add("Pincode '" + pin + "' is invalid (must be exactly 6 digits)");
            return;
        }

        if (processedInBatch.contains(pin)) {
            skippedPincodes.add(pin + " (duplicate in batch)");
            return;
        }

        if (pincodeRepository.existsByPincode(pin)) {
            skippedPincodes.add(pin + " (already registered)");
            return;
        }

        processedInBatch.add(pin);
        toSave.add(ServiceablePincode.builder()
                .pincode(pin)
                .city(city)
                .estimatedDeliveryDays(deliveryDays != null && deliveryDays > 0 ? deliveryDays : 3)
                .codAvailable(cod != null ? cod : true)
                .isActive(active != null ? active : true)
                .build());
        addedPincodes.add(pin);
    }

    private PincodeResponseDTO mapToDTO(ServiceablePincode pincode) {
        return PincodeResponseDTO.builder()
                .id(pincode.getId())
                .pincode(pincode.getPincode())
                .cityId(pincode.getCity() != null ? pincode.getCity().getId() : null)
                .cityName(pincode.getCity() != null ? pincode.getCity().getName() : null)
                .state(pincode.getCity() != null ? pincode.getCity().getState() : null)
                .estimatedDeliveryDays(pincode.getEstimatedDeliveryDays())
                .codAvailable(pincode.getCodAvailable())
                .isActive(pincode.getIsActive())
                .build();
    }
}
