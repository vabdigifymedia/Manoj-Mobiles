package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.deliverypartner.CreateDeliveryPartnerRequestDTO;
import com.api.manojmobiles.dto.deliverypartner.DeliveryPartnerResponseDTO;
import com.api.manojmobiles.dto.deliverypartner.UpdateDeliveryPartnerRequestDTO;
import com.api.manojmobiles.service.DeliveryPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/delivery-partners")
@RequiredArgsConstructor
@Tag(name = "Admin Delivery Partner")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryPartnerController {

    private final DeliveryPartnerService deliveryPartnerService;

    @PostMapping
    @Operation(summary = "Create a new delivery partner")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponseDTO>> createDeliveryPartner(
            @Valid @RequestBody CreateDeliveryPartnerRequestDTO request) {
        DeliveryPartnerResponseDTO response = deliveryPartnerService.createDeliveryPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delivery partner created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all delivery partners with pagination and search")
    public ResponseEntity<ApiResponse<Page<DeliveryPartnerResponseDTO>>> getAllDeliveryPartners(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DeliveryPartnerResponseDTO> response = deliveryPartnerService.getAllDeliveryPartners(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Delivery partners fetched successfully", response));
    }

    @GetMapping("/active")
    @Operation(summary = "Get list of all active delivery partners (without pagination)")
    public ResponseEntity<ApiResponse<List<DeliveryPartnerResponseDTO>>> getActiveDeliveryPartners() {
        List<DeliveryPartnerResponseDTO> response = deliveryPartnerService.getActiveDeliveryPartners();
        return ResponseEntity.ok(ApiResponse.success("Active delivery partners fetched successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery partner by ID")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponseDTO>> getDeliveryPartnerById(@PathVariable UUID id) {
        DeliveryPartnerResponseDTO response = deliveryPartnerService.getDeliveryPartnerById(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery partner fetched successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update delivery partner details")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponseDTO>> updateDeliveryPartner(
            @PathVariable UUID id,
            @RequestBody UpdateDeliveryPartnerRequestDTO request) {
        DeliveryPartnerResponseDTO response = deliveryPartnerService.updateDeliveryPartner(id, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery partner updated successfully", response));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle active status of a delivery partner")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponseDTO>> toggleActiveStatus(@PathVariable UUID id) {
        DeliveryPartnerResponseDTO response = deliveryPartnerService.toggleActiveStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery partner status toggled successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete delivery partner")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryPartner(@PathVariable UUID id) {
        deliveryPartnerService.deleteDeliveryPartner(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery partner deleted successfully", null));
    }
}
