package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.brand.BrandRequestDTO;
import com.api.manojmobiles.dto.brand.BrandResponseDTO;
import com.api.manojmobiles.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@Tag(name = "Brand")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // Public (read) endpoints

    @GetMapping("/api/public/brands")
    public ResponseEntity<ApiResponse<List<BrandResponseDTO>>> getAllBrands() {
        List<BrandResponseDTO> brands = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success("Brands fetched successfully", brands));
    }

    @GetMapping("/api/public/brands/{id}")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> getBrandById(@PathVariable UUID id) {
        BrandResponseDTO brand = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success("Brand fetched successfully", brand));
    }

    @GetMapping("/api/public/brands/slug/{slug}")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> getBrandBySlug(@PathVariable String slug) {
        BrandResponseDTO brand = brandService.getBrandBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Brand fetched successfully", brand));
    }

    // Admin (write) endpoints

    @PostMapping("/api/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> createBrand(
            @Valid @RequestBody BrandRequestDTO request) {
        BrandResponseDTO created = brandService.createBrand(request);
        return new ResponseEntity<>(ApiResponse.success("Brand created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/api/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> updateBrand(
            @PathVariable UUID id,
            @Valid @RequestBody BrandRequestDTO request) {
        BrandResponseDTO updated = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.success("Brand updated successfully", updated));
    }

    @DeleteMapping("/api/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable UUID id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand deleted successfully", null));
    }
}
