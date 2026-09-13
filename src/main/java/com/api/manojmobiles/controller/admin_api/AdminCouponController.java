package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.coupon.CouponResponseDTO;
import com.api.manojmobiles.dto.coupon.CouponUsageDTO;
import com.api.manojmobiles.dto.coupon.CreateCouponRequestDTO;
import com.api.manojmobiles.dto.coupon.UpdateCouponRequestDTO;
import com.api.manojmobiles.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon (Admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    @Operation(summary = "Create a new coupon")
    public ResponseEntity<ApiResponse<CouponResponseDTO>> createCoupon(@Valid @RequestBody CreateCouponRequestDTO request) {
        CouponResponseDTO coupon = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created successfully", coupon));
    }

    @GetMapping
    @Operation(summary = "Get all coupons")
    public ResponseEntity<ApiResponse<List<CouponResponseDTO>>> getAllCoupons() {
        List<CouponResponseDTO> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponse.success("Coupons fetched successfully", coupons));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by ID")
    public ResponseEntity<ApiResponse<CouponResponseDTO>> getCouponById(@PathVariable UUID id) {
        CouponResponseDTO coupon = couponService.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon fetched successfully", coupon));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coupon details")
    public ResponseEntity<ApiResponse<CouponResponseDTO>> updateCoupon(@PathVariable UUID id, @Valid @RequestBody UpdateCouponRequestDTO request) {
        CouponResponseDTO coupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponse.success("Coupon updated successfully", coupon));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or soft-delete a coupon")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable UUID id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted/deactivated successfully", null));
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Get coupon usage stats")
    public ResponseEntity<ApiResponse<List<CouponUsageDTO>>> getCouponUsageStats(@PathVariable UUID id) {
        List<CouponUsageDTO> stats = couponService.getCouponUsageStats(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon usage stats fetched successfully", stats));
    }
}
