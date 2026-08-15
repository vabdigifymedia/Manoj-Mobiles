package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.response.BannerResponseDTO;
import com.api.manojmobiles.entity.enums.BannerType;
import com.api.manojmobiles.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/banners")
@RequiredArgsConstructor
@Tag(name = "Public Banner", description = "Endpoints for fetching active banners")
public class PublicBannerController {

    private final BannerService bannerService;

    @GetMapping
    @Operation(summary = "Get active banners", description = "Fetches all active banners, optionally filtered by type")
    public ResponseEntity<ApiResponse<List<BannerResponseDTO>>> getActiveBanners(
            @RequestParam(value = "type", required = false) BannerType type) {
        List<BannerResponseDTO> banners = bannerService.getActiveBanners(type);
        return ResponseEntity.ok(ApiResponse.success("Active banners retrieved successfully", banners));
    }
}
