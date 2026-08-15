package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.request.StoreSettingRequestDTO;
import com.api.manojmobiles.dto.response.StoreSettingResponseDTO;
import com.api.manojmobiles.service.StoreSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Store Settings", description = "Endpoints for managing store settings")
public class AdminStoreSettingController {

    private final StoreSettingService storeSettingService;

    @GetMapping
    @Operation(summary = "Get store settings", description = "Fetches the current store settings")
    public ResponseEntity<ApiResponse<StoreSettingResponseDTO>> getStoreSettings() {
        StoreSettingResponseDTO settings = storeSettingService.getStoreSettings();
        return ResponseEntity.ok(ApiResponse.success("Store settings retrieved successfully", settings));
    }

    @PutMapping
    @Operation(summary = "Update store settings", description = "Updates the store settings")
    public ResponseEntity<ApiResponse<StoreSettingResponseDTO>> updateStoreSettings(
            @Valid @RequestBody StoreSettingRequestDTO dto) {
        StoreSettingResponseDTO updated = storeSettingService.updateStoreSettings(dto);
        return ResponseEntity.ok(ApiResponse.success("Store settings updated successfully", updated));
    }
}
