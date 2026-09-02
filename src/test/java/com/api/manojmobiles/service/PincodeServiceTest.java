package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.pincode.*;
import com.api.manojmobiles.entity.City;
import com.api.manojmobiles.entity.ServiceablePincode;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.repository.CityRepository;
import com.api.manojmobiles.repository.ServiceablePincodeRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PincodeServiceTest {

    @Mock
    private ServiceablePincodeRepository pincodeRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private PincodeService pincodeService;

    private City testCity;
    private ServiceablePincode testPincode;
    private UUID cityId;
    private UUID pincodeId;

    @BeforeEach
    void setUp() {
        cityId = UUID.randomUUID();
        pincodeId = UUID.randomUUID();

        testCity = City.builder()
                .id(cityId)
                .name("Faridabad")
                .state("Haryana")
                .isActive(true)
                .build();

        testPincode = ServiceablePincode.builder()
                .id(pincodeId)
                .pincode("121001")
                .city(testCity)
                .estimatedDeliveryDays(2)
                .codAvailable(true)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Check pincode returns isServiceable=false when pincode isActive=false")
    void checkPincode_whenInactive_returnsNotServiceable() {
        testPincode.setIsActive(false);
        when(pincodeRepository.findByPincode("121001")).thenReturn(Optional.of(testPincode));

        PincodeCheckResponseDTO response = pincodeService.checkPincodeServiceability("121001");

        assertFalse(response.isServiceable());
        assertNull(response.getEstimatedDeliveryDays());
        assertNull(response.getCodAvailable());
    }

    @Test
    @DisplayName("Check pincode returns isServiceable=true when pincode and city are active")
    void checkPincode_whenActive_returnsServiceable() {
        when(pincodeRepository.findByPincode("121001")).thenReturn(Optional.of(testPincode));

        PincodeCheckResponseDTO response = pincodeService.checkPincodeServiceability("121001");

        assertTrue(response.isServiceable());
        assertEquals("Faridabad", response.getCityName());
        assertEquals("Haryana", response.getState());
        assertEquals(2, response.getEstimatedDeliveryDays());
        assertTrue(response.getCodAvailable());
    }

    @Test
    @DisplayName("Toggle pincode status toggles isActive flag")
    void togglePincodeStatus_flipsIsActive() {
        when(pincodeRepository.findById(pincodeId)).thenReturn(Optional.of(testPincode));
        when(pincodeRepository.save(any(ServiceablePincode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PincodeResponseDTO response = pincodeService.togglePincodeStatus(pincodeId);

        assertFalse(response.getIsActive());

        // Toggle again
        PincodeResponseDTO response2 = pincodeService.togglePincodeStatus(pincodeId);
        assertTrue(response2.getIsActive());
    }

    @Test
    @DisplayName("Update pincode allows changing pincode string and cityId")
    void updatePincode_fullEditPincodeAndCity() {
        UUID newCityId = UUID.randomUUID();
        City newCity = City.builder()
                .id(newCityId)
                .name("Gurugram")
                .state("Haryana")
                .isActive(true)
                .build();

        UpdatePincodeRequestDTO updateRequest = UpdatePincodeRequestDTO.builder()
                .pincode("122001")
                .cityId(newCityId)
                .estimatedDeliveryDays(3)
                .codAvailable(false)
                .isActive(true)
                .build();

        when(pincodeRepository.findById(pincodeId)).thenReturn(Optional.of(testPincode));
        when(pincodeRepository.existsByPincode("122001")).thenReturn(false);
        when(cityRepository.findById(newCityId)).thenReturn(Optional.of(newCity));
        when(pincodeRepository.save(any(ServiceablePincode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PincodeResponseDTO result = pincodeService.updatePincode(pincodeId, updateRequest);

        assertEquals("122001", result.getPincode());
        assertEquals(newCityId, result.getCityId());
        assertEquals("Gurugram", result.getCityName());
        assertEquals(3, result.getEstimatedDeliveryDays());
        assertFalse(result.getCodAvailable());
    }

    @Test
    @DisplayName("Update pincode throws error if updated pincode already exists on another record")
    void updatePincode_pincodeConflictThrowsException() {
        UpdatePincodeRequestDTO updateRequest = UpdatePincodeRequestDTO.builder()
                .pincode("122001")
                .build();

        when(pincodeRepository.findById(pincodeId)).thenReturn(Optional.of(testPincode));
        when(pincodeRepository.existsByPincode("122001")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> pincodeService.updatePincode(pincodeId, updateRequest));
    }

    @Test
    @DisplayName("getAllPincodes with pagination and search calls searchPincodes repository method")
    void getAllPincodes_withSearchAndPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ServiceablePincode> page = new PageImpl<>(List.of(testPincode), pageable, 1);

        when(pincodeRepository.searchPincodes(eq(cityId), eq("121"), eq(pageable))).thenReturn(page);

        Page<PincodeResponseDTO> result = pincodeService.getAllPincodes(cityId, "121", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("121001", result.getContent().get(0).getPincode());
    }

    @Test
    @DisplayName("Bulk upload pincodes via JSON skips duplicates and saves valid ones")
    void bulkUploadPincodes_jsonWithDuplicates() {
        BulkPincodeUploadRequestDTO request = BulkPincodeUploadRequestDTO.builder()
                .cityId(cityId)
                .pincodes(List.of("121001", "121002", "121002", "invalid", "121003"))
                .estimatedDeliveryDays(2)
                .codAvailable(true)
                .build();

        when(cityRepository.findById(cityId)).thenReturn(Optional.of(testCity));
        // "121001" already exists
        when(pincodeRepository.existsByPincode("121001")).thenReturn(true);
        when(pincodeRepository.existsByPincode("121002")).thenReturn(false);
        when(pincodeRepository.existsByPincode("121003")).thenReturn(false);

        BulkPincodeResponseDTO response = pincodeService.bulkUploadPincodes(request);

        assertEquals(2, response.getAddedCount()); // 121002, 121003
        assertEquals(2, response.getSkippedCount()); // 121001 (already exists), 121002 (duplicate in batch)
        assertEquals(1, response.getFailedCount()); // invalid (not 6 digits)
        verify(pincodeRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Bulk upload pincodes via CSV parses lines, skips header, and saves valid entries")
    void bulkUploadPincodesFromCsv() {
        String csvContent = "pincode,estimatedDeliveryDays,codAvailable\n" +
                "121004,2,true\n" +
                "121005,3,false\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pincodes.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(cityRepository.findById(cityId)).thenReturn(Optional.of(testCity));
        when(pincodeRepository.existsByPincode("121004")).thenReturn(false);
        when(pincodeRepository.existsByPincode("121005")).thenReturn(false);

        BulkPincodeResponseDTO response = pincodeService.bulkUploadPincodesFromCsv(file, cityId, 3, true);

        assertEquals(2, response.getAddedCount());
        assertEquals(0, response.getSkippedCount());
        assertEquals(0, response.getFailedCount());
        verify(pincodeRepository, times(1)).saveAll(any());
    }
}
