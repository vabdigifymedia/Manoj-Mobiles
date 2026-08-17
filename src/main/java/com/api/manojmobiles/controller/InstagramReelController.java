package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.reel.InstagramReelRequestDTO;
import com.api.manojmobiles.dto.reel.InstagramReelResponseDTO;
import com.api.manojmobiles.service.InstagramReelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InstagramReelController {

    private final InstagramReelService reelService;

    // Public endpoint for the Home Page
    @GetMapping("/api/public/reels")
    public ResponseEntity<ApiResponse<List<InstagramReelResponseDTO>>> getActiveReels() {
        List<InstagramReelResponseDTO> reels = reelService.getAllActiveReels();
        return ResponseEntity.ok(ApiResponse.success("Reels fetched successfully", reels));
    }

    // Admin endpoints
    @GetMapping("/api/reels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InstagramReelResponseDTO>>> getAllReelsForAdmin() {
        List<InstagramReelResponseDTO> reels = reelService.getAllReelsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("All reels fetched successfully", reels));
    }

    @PostMapping("/api/reels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstagramReelResponseDTO>> createReel(@Valid @RequestBody InstagramReelRequestDTO request) {
        InstagramReelResponseDTO reel = reelService.createReel(request);
        return new ResponseEntity<>(ApiResponse.success("Reel created successfully", reel), HttpStatus.CREATED);
    }

    @PutMapping("/api/reels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstagramReelResponseDTO>> updateReel(
            @PathVariable UUID id, 
            @Valid @RequestBody InstagramReelRequestDTO request) {
        InstagramReelResponseDTO reel = reelService.updateReel(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reel updated successfully", reel));
    }

    @DeleteMapping("/api/reels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReel(@PathVariable UUID id) {
        reelService.deleteReel(id);
        return ResponseEntity.ok(ApiResponse.success("Reel deleted successfully", null));
    }
}
