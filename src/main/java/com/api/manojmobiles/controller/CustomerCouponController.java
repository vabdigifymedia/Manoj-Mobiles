package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.coupon.ActiveCouponResponseDTO;
import com.api.manojmobiles.dto.coupon.ApplyCouponRequestDTO;
import com.api.manojmobiles.dto.coupon.ApplyCouponResponseDTO;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.UserRepository;
import com.api.manojmobiles.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/user/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon (Customer)")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerCouponController {

    private final CouponService couponService;
    private final UserRepository userRepository;

    @GetMapping("/active")
    @Operation(summary = "List active coupons with alreadyUsedByYou flag")
    public ResponseEntity<ApiResponse<List<ActiveCouponResponseDTO>>> getActiveCoupons(Principal principal) {
        User user = getUser(principal);
        List<ActiveCouponResponseDTO> coupons = couponService.getActiveCouponsForUser(user);
        return ResponseEntity.ok(ApiResponse.success("Active coupons fetched successfully", coupons));
    }

    @PostMapping("/apply")
    @Operation(summary = "Preview and validate coupon discount on cart")
    public ResponseEntity<ApiResponse<ApplyCouponResponseDTO>> applyCoupon(
            @Valid @RequestBody ApplyCouponRequestDTO request,
            Principal principal) {
        User user = getUser(principal);
        ApplyCouponResponseDTO response = couponService.previewCouponDiscount(request.getCode(), request.getCartTotal(), user);
        return ResponseEntity.ok(ApiResponse.success("Coupon preview calculated successfully", response));
    }

    private User getUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseGet(() -> userRepository.findByPhone(principal.getName())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }
}
