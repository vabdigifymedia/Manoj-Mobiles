package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.delivery.AssignShipmentRequestDTO;
import com.api.manojmobiles.dto.delivery.DeliveryAgentRequestDTO;
import com.api.manojmobiles.dto.delivery.DeliveryAgentResponseDTO;
import com.api.manojmobiles.dto.delivery.ShipmentResponseDTO;
import com.api.manojmobiles.service.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Delivery Management")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/delivery-agents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryAgentResponseDTO>> createDeliveryAgent(
            @Valid @RequestBody DeliveryAgentRequestDTO request) {
        DeliveryAgentResponseDTO agent = deliveryService.createDeliveryAgent(request);
        return new ResponseEntity<>(ApiResponse.success("Delivery agent created successfully", agent), HttpStatus.CREATED);
    }

    @GetMapping("/delivery-agents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<DeliveryAgentResponseDTO>>> getAllDeliveryAgents(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DeliveryAgentResponseDTO> agents = deliveryService.getAllAgents(pageable);
        return ResponseEntity.ok(ApiResponse.success("Delivery agents fetched successfully", agents));
    }

    @PutMapping("/delivery-agents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryAgentResponseDTO>> updateDeliveryAgent(
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryAgentRequestDTO request) {
        DeliveryAgentResponseDTO updatedAgent = deliveryService.updateDeliveryAgent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery agent updated successfully", updatedAgent));
    }

    @DeleteMapping("/delivery-agents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryAgent(@PathVariable UUID id) {
        deliveryService.deleteDeliveryAgent(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery agent deleted successfully", null));
    }

    @PostMapping("/orders/{orderId}/assign-agent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShipmentResponseDTO>> assignAgentToOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody AssignShipmentRequestDTO request) {
        ShipmentResponseDTO shipment = deliveryService.assignShipment(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order assigned to agent successfully", shipment));
    }
}
