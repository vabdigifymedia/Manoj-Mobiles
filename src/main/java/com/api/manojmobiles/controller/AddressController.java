package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.address.AddressRequestDTO;
import com.api.manojmobiles.dto.address.AddressResponseDTO;
import com.api.manojmobiles.service.AddressService;
import com.api.manojmobiles.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@Tag(name = "Address")
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final RateLimiterService rateLimiterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AddressResponseDTO>>> getAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        List<AddressResponseDTO> addresses = addressService.getUserAddresses(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched successfully", addresses));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> createAddress(
            @Valid @RequestBody AddressRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        // Rate limiting to prevent address spam
        String ip = getClientIp(httpRequest);
        rateLimiterService.checkRateLimit(
                "rate:address:write:" + ip,
                10,
                Duration.ofMinutes(1)
        );

        AddressResponseDTO created = addressService.createAddress(userDetails.getUsername(), request);
        return new ResponseEntity<>(ApiResponse.success("Address created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        // Rate limiting to prevent update spam
        String ip = getClientIp(httpRequest);
        rateLimiterService.checkRateLimit(
                "rate:address:write:" + ip,
                10,
                Duration.ofMinutes(1)
        );

        AddressResponseDTO updated = addressService.updateAddress(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        addressService.deleteAddress(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
