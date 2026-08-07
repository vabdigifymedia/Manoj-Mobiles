package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.order.OrderResponseDTO;
import com.api.manojmobiles.dto.order.PlaceOrderRequestDTO;
import com.api.manojmobiles.service.OrderService;
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

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
@Tag(name = "Order (Customer)")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> placeOrder(
            Principal principal,
            @Valid @RequestBody PlaceOrderRequestDTO request) {
        OrderResponseDTO order = orderService.placeOrder(principal.getName(), request);
        return new ResponseEntity<>(ApiResponse.success("Order placed successfully", order), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get user's order history")
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> getUserOrders(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<OrderResponseDTO> orders = orderService.getUserOrders(principal.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(
            Principal principal,
            @PathVariable UUID orderId) {
        OrderResponseDTO order = orderService.getOrderById(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order details fetched successfully", order));
    }

    @PostMapping("/{orderId}/mock-payment")
    @Operation(summary = "Mock endpoint to simulate successful payment for online orders")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> mockPayment(
            Principal principal,
            @PathVariable UUID orderId) {
        OrderResponseDTO updatedOrder = orderService.mockPayment(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment successful", updatedOrder));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order before delivery")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(
            Principal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody com.api.manojmobiles.dto.order.CancelOrderRequestDTO request) {
        OrderResponseDTO cancelledOrder = orderService.cancelOrder(principal.getName(), orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelledOrder));
    }
}
