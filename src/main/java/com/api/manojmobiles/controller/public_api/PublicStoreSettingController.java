package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.response.StoreSettingResponseDTO;
import com.api.manojmobiles.service.StoreSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/settings")
@RequiredArgsConstructor
@Tag(name = "Public Store Settings", description = "Endpoints for fetching store configuration")
public class PublicStoreSettingController {

    private final StoreSettingService storeSettingService;

    @GetMapping
    @Operation(summary = "Get store settings", description = "Fetches the current store settings")
    public ResponseEntity<ApiResponse<StoreSettingResponseDTO>> getStoreSettings() {
        StoreSettingResponseDTO settings = storeSettingService.getStoreSettings();
        return ResponseEntity.ok(ApiResponse.success("Store settings retrieved successfully", settings));
    }
}
