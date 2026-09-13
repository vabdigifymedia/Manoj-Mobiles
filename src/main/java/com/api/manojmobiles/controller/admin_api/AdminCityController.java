package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.city.CityResponseDTO;
import com.api.manojmobiles.dto.city.CreateCityRequestDTO;
import com.api.manojmobiles.dto.city.UpdateCityRequestDTO;
import com.api.manojmobiles.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/cities")
@RequiredArgsConstructor
@Tag(name = "City (Admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCityController {

    private final CityService cityService;

    @PostMapping
    @Operation(summary = "Add a new city")
    public ResponseEntity<ApiResponse<CityResponseDTO>> createCity(@Valid @RequestBody CreateCityRequestDTO request) {
        CityResponseDTO city = cityService.createCity(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("City added successfully", city));
    }

    @GetMapping
    @Operation(summary = "Get all cities with pagination and search")
    public ResponseEntity<ApiResponse<Page<CityResponseDTO>>> getAllCities(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<CityResponseDTO> cities = cityService.getAllCities(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Cities fetched successfully", cities));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active cities (unpaginated for dropdowns)")
    public ResponseEntity<ApiResponse<List<CityResponseDTO>>> getActiveCities() {
        List<CityResponseDTO> cities = cityService.getActiveCities();
        return ResponseEntity.ok(ApiResponse.success("Active cities fetched successfully", cities));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a city by ID")
    public ResponseEntity<ApiResponse<CityResponseDTO>> getCityById(@PathVariable UUID id) {
        CityResponseDTO city = cityService.getCityById(id);
        return ResponseEntity.ok(ApiResponse.success("City fetched successfully", city));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a city")
    public ResponseEntity<ApiResponse<CityResponseDTO>> updateCity(@PathVariable UUID id, @Valid @RequestBody UpdateCityRequestDTO request) {
        CityResponseDTO city = cityService.updateCity(id, request);
        return ResponseEntity.ok(ApiResponse.success("City updated successfully", city));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a city")
    public ResponseEntity<ApiResponse<Void>> deleteCity(@PathVariable UUID id) {
        cityService.deleteCity(id);
        return ResponseEntity.ok(ApiResponse.success("City deleted successfully", null));
    }
}
