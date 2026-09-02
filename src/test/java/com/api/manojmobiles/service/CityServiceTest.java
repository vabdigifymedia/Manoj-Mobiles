package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.city.CityResponseDTO;
import com.api.manojmobiles.entity.City;
import com.api.manojmobiles.repository.CityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityService cityService;

    @Test
    @DisplayName("getAllCities with search and pageable delegates to cityRepository.searchCities")
    void getAllCities_searchAndPageable() {
        City city = City.builder()
                .id(UUID.randomUUID())
                .name("Faridabad")
                .state("Haryana")
                .isActive(true)
                .pincodes(new ArrayList<>())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<City> page = new PageImpl<>(List.of(city), pageable, 1);

        when(cityRepository.searchCities(eq("Faridabad"), eq(pageable))).thenReturn(page);

        Page<CityResponseDTO> result = cityService.getAllCities("Faridabad", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Faridabad", result.getContent().get(0).getName());
    }
}
