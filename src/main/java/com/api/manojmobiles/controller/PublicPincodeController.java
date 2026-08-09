package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.pincode.PincodeCheckResponseDTO;
import com.api.manojmobiles.service.PincodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/pincode")
@RequiredArgsConstructor
@Tag(name = "Public Pincode Check")
public class PublicPincodeController {

    private final PincodeService pincodeService;

    @GetMapping("/check/{pincode}")
    @Operation(summary = "Check if a pincode is serviceable")
    public ResponseEntity<ApiResponse<PincodeCheckResponseDTO>> checkPincode(@PathVariable String pincode) {
        PincodeCheckResponseDTO result = pincodeService.checkPincodeServiceability(pincode);
        String message = result.isServiceable() ? "Pincode is serviceable" : "Delivery is not available at this pincode";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
