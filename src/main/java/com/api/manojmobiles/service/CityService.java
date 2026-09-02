package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.city.CityResponseDTO;
import com.api.manojmobiles.dto.city.CreateCityRequestDTO;
import com.api.manojmobiles.dto.city.UpdateCityRequestDTO;
import com.api.manojmobiles.entity.City;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional
    public CityResponseDTO createCity(CreateCityRequestDTO request) {
        if (cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(request.getName(), request.getState())) {
            throw new BadRequestException("City '" + request.getName() + "' already exists in state '" + request.getState() + "'");
        }

        City city = City.builder()
                .name(request.getName())
                .state(request.getState())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        city = cityRepository.save(city);
        return mapToDTO(city);
    }

    @Transactional(readOnly = true)
    public List<CityResponseDTO> getAllCities() {
        return cityRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CityResponseDTO> getAllCities(String search, Pageable pageable) {
        return cityRepository.searchCities(search, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<CityResponseDTO> getActiveCities() {
        return cityRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CityResponseDTO getCityById(UUID id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));
        return mapToDTO(city);
    }

    @Transactional
    public CityResponseDTO updateCity(UUID id, UpdateCityRequestDTO request) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));

        if (!city.getName().equalsIgnoreCase(request.getName()) || !city.getState().equalsIgnoreCase(request.getState())) {
            if (cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(request.getName(), request.getState())) {
                throw new BadRequestException("City '" + request.getName() + "' already exists in state '" + request.getState() + "'");
            }
        }

        city.setName(request.getName());
        city.setState(request.getState());
        
        if (request.getIsActive() != null) {
            city.setIsActive(request.getIsActive());
        }

        city = cityRepository.save(city);
        return mapToDTO(city);
    }

    @Transactional
    public void deleteCity(UUID id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));
        
        if (city.getPincodes() != null && !city.getPincodes().isEmpty()) {
            throw new BadRequestException("Cannot delete city as it has associated pincodes. Please delete or reassign pincodes first, or mark the city as inactive.");
        }
        
        cityRepository.delete(city);
    }

    private CityResponseDTO mapToDTO(City city) {
        long pincodeCount = city.getPincodes() != null ? city.getPincodes().size() : 0;
        return CityResponseDTO.builder()
                .id(city.getId())
                .name(city.getName())
                .state(city.getState())
                .isActive(city.getIsActive())
                .totalPincodesCount(pincodeCount)
                .build();
    }
}
