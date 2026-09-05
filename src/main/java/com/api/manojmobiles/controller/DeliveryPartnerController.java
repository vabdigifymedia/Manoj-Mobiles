package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.order.UpdateLiveLocationRequestDTO;
import com.api.manojmobiles.service.LiveLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/delivery/orders")
@RequiredArgsConstructor
@Tag(name = "Delivery Partner API")
public class DeliveryPartnerController {

    private final LiveLocationService liveLocationService;

    @PostMapping("/{orderId}/live-location")
    @Operation(summary = "Update live location of an order (used by Delivery Partner App)")
    public ResponseEntity<ApiResponse<Void>> updateLiveLocation(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateLiveLocationRequestDTO request) {
        liveLocationService.updateLiveLocation(orderId, request.getLat(), request.getLng());
        return ResponseEntity.ok(ApiResponse.success("Live location updated successfully", null));
    }
}
