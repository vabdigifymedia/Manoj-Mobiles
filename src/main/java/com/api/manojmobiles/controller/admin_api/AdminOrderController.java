package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.order.OrderResponseDTO;
import com.api.manojmobiles.dto.order.UpdateOrderStatusRequestDTO;
import com.api.manojmobiles.service.OrderService;
import com.api.manojmobiles.service.LiveLocationService;
import com.api.manojmobiles.dto.order.LiveLocationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Order (Admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final LiveLocationService liveLocationService;

    @GetMapping
    @Operation(summary = "Get all orders with pagination")
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<OrderResponseDTO> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("All orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID (Admin)")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable UUID orderId) {
        OrderResponseDTO order = orderService.getOrderByIdForAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully.", order));
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(
            Principal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequestDTO request) {
        OrderResponseDTO updatedOrder = orderService.updateOrderStatus(orderId, request.getStatus(), request.getNote(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully.", updatedOrder));
    }

    @PostMapping("/{orderId}/assign-partner")
    @Operation(summary = "Assign a delivery partner to a hyperlocal order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> assignDeliveryPartner(
            @PathVariable UUID orderId,
            @Valid @RequestBody com.api.manojmobiles.dto.order.AssignDeliveryPartnerRequestDTO request) {
        OrderResponseDTO updatedOrder = orderService.assignDeliveryPartner(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery partner assigned successfully.", updatedOrder));
    }

    @GetMapping("/{orderId}/live-location")
    @Operation(summary = "Track the live location of an OUT_FOR_DELIVERY order")
    public ResponseEntity<ApiResponse<LiveLocationResponseDTO>> getLiveLocation(
            @PathVariable UUID orderId) {
        
        LiveLocationResponseDTO location = liveLocationService.getLiveLocation(orderId);
        if (location == null) {
            return ResponseEntity.ok(ApiResponse.success("Live location not available yet", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Live location fetched successfully", location));
    }
}
