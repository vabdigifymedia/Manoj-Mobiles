package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.delivery.DeliveryOptionsResponseDTO;
import com.api.manojmobiles.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/public/delivery")
@RequiredArgsConstructor
@Tag(name = "Public Delivery Options")
public class PublicDeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/options")
    @Operation(summary = "Check available delivery options (Hyperlocal vs Standard) with charges and ETAs")
    public ResponseEntity<ApiResponse<DeliveryOptionsResponseDTO>> getDeliveryOptions(
            @RequestParam String pincode,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) BigDecimal cartTotal) {
        DeliveryOptionsResponseDTO options = deliveryService.getDeliveryOptions(pincode, lat, lng, cartTotal);
        return ResponseEntity.ok(ApiResponse.success("Delivery options fetched successfully", options));
    }
}
