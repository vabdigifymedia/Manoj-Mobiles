package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.request.BannerRequestDTO;
import com.api.manojmobiles.dto.request.ReorderRequestDTO;
import com.api.manojmobiles.dto.response.BannerResponseDTO;
import com.api.manojmobiles.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Banner Management", description = "Endpoints for managing banners")
public class AdminBannerController {

    private final BannerService bannerService;

    @GetMapping
    @Operation(summary = "Get all banners", description = "Fetches all banners including inactive ones")
    public ResponseEntity<ApiResponse<List<BannerResponseDTO>>> getAllBanners() {
        List<BannerResponseDTO> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(ApiResponse.success("Banners retrieved successfully", banners));
    }

    @PostMapping
    @Operation(summary = "Create banner", description = "Creates a new banner")
    public ResponseEntity<ApiResponse<BannerResponseDTO>> createBanner(@Valid @RequestBody BannerRequestDTO dto) {
        BannerResponseDTO created = bannerService.createBanner(dto);
        return ResponseEntity.ok(ApiResponse.success("Banner created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update banner", description = "Updates an existing banner")
    public ResponseEntity<ApiResponse<BannerResponseDTO>> updateBanner(
            @PathVariable UUID id, @Valid @RequestBody BannerRequestDTO dto) {
        BannerResponseDTO updated = bannerService.updateBanner(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Banner updated successfully", updated));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update banner status", description = "Toggles banner active status")
    public ResponseEntity<ApiResponse<Void>> updateBannerStatus(
            @PathVariable UUID id, @RequestParam boolean active) {
        bannerService.updateBannerStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("Banner status updated successfully", null));
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder banners", description = "Updates the display order of banners")
    public ResponseEntity<ApiResponse<Void>> reorderBanners(@Valid @RequestBody ReorderRequestDTO dto) {
        bannerService.reorderBanners(dto);
        return ResponseEntity.ok(ApiResponse.success("Banners reordered successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete banner", description = "Deletes a banner")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted successfully", null));
    }
}
