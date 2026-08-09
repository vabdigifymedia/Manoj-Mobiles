package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.pincode.CreatePincodeRequestDTO;
import com.api.manojmobiles.dto.pincode.PincodeCheckResponseDTO;
import com.api.manojmobiles.dto.pincode.PincodeResponseDTO;
import com.api.manojmobiles.dto.pincode.UpdatePincodeRequestDTO;
import com.api.manojmobiles.entity.City;
import com.api.manojmobiles.entity.ServiceablePincode;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CityRepository;
import com.api.manojmobiles.repository.ServiceablePincodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PincodeService {

    private final ServiceablePincodeRepository pincodeRepository;
    private final CityRepository cityRepository;

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

        if (request.getEstimatedDeliveryDays() != null) {
            pincode.setEstimatedDeliveryDays(request.getEstimatedDeliveryDays());
        }
        if (request.getCodAvailable() != null) {
            pincode.setCodAvailable(request.getCodAvailable());
        }

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
                    boolean isActiveCity = Boolean.TRUE.equals(pincode.getCity().getIsActive());
                    return PincodeCheckResponseDTO.builder()
                            .pincode(pincode.getPincode())
                            .isServiceable(isActiveCity)
                            .cityName(pincode.getCity().getName())
                            .state(pincode.getCity().getState())
                            .estimatedDeliveryDays(isActiveCity ? pincode.getEstimatedDeliveryDays() : null)
                            .codAvailable(isActiveCity ? pincode.getCodAvailable() : null)
                            .build();
                })
                .orElseGet(() -> PincodeCheckResponseDTO.builder()
                        .pincode(pincodeStr)
                        .isServiceable(false)
                        .build());
    }

    private PincodeResponseDTO mapToDTO(ServiceablePincode pincode) {
        return PincodeResponseDTO.builder()
                .id(pincode.getId())
                .pincode(pincode.getPincode())
                .cityId(pincode.getCity().getId())
                .cityName(pincode.getCity().getName())
                .state(pincode.getCity().getState())
                .estimatedDeliveryDays(pincode.getEstimatedDeliveryDays())
                .codAvailable(pincode.getCodAvailable())
                .build();
    }
}
