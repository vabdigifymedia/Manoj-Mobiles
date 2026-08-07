package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.delivery.AvailabilityRequestDTO;
import com.api.manojmobiles.dto.delivery.DeliveryAgentResponseDTO;
import com.api.manojmobiles.dto.delivery.LocationUpdateRequestDTO;
import com.api.manojmobiles.dto.delivery.ShipmentResponseDTO;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.service.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent Delivery Module")
public class AgentDeliveryController {

    private final DeliveryService deliveryService;

    @PutMapping("/availability")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<ApiResponse<DeliveryAgentResponseDTO>> updateAvailability(
            @Valid @RequestBody AvailabilityRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        DeliveryAgentResponseDTO agent = deliveryService.updateAgentAvailability(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Availability updated successfully", agent));
    }

    @GetMapping("/shipments")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<ApiResponse<Page<ShipmentResponseDTO>>> getMyShipments(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        Page<ShipmentResponseDTO> shipments = deliveryService.getAgentShipments(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Shipments fetched successfully", shipments));
    }

    @PutMapping("/shipments/{shipmentId}/status")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<ApiResponse<ShipmentResponseDTO>> updateShipmentStatus(
            @PathVariable UUID shipmentId,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        ShipmentResponseDTO shipment = deliveryService.updateShipmentStatus(userDetails.getUsername(), shipmentId, status);
        return ResponseEntity.ok(ApiResponse.success("Shipment status updated successfully", shipment));
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<ApiResponse<Void>> pushLocation(
            @Valid @RequestBody LocationUpdateRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        deliveryService.updateLocation(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", null));
    }
}
