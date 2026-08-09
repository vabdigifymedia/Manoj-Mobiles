package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.pincode.CreatePincodeRequestDTO;
import com.api.manojmobiles.dto.pincode.PincodeResponseDTO;
import com.api.manojmobiles.dto.pincode.UpdatePincodeRequestDTO;
import com.api.manojmobiles.service.PincodeService;
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
@RequestMapping("/api/admin/pincodes")
@RequiredArgsConstructor
@Tag(name = "Serviceable Pincode (Admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPincodeController {

    private final PincodeService pincodeService;

    @PostMapping
    @Operation(summary = "Add a new serviceable pincode")
    public ResponseEntity<ApiResponse<PincodeResponseDTO>> createPincode(@Valid @RequestBody CreatePincodeRequestDTO request) {
        PincodeResponseDTO pincode = pincodeService.createPincode(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pincode added successfully", pincode));
    }

    @GetMapping
    @Operation(summary = "Get all serviceable pincodes")
    public ResponseEntity<ApiResponse<List<PincodeResponseDTO>>> getAllPincodes(
            @RequestParam(required = false) UUID cityId) {
        List<PincodeResponseDTO> pincodes;
        if (cityId != null) {
            pincodes = pincodeService.getPincodesByCity(cityId);
        } else {
            pincodes = pincodeService.getAllPincodes();
        }
        return ResponseEntity.ok(ApiResponse.success("Pincodes fetched successfully", pincodes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pincode by ID")
    public ResponseEntity<ApiResponse<PincodeResponseDTO>> getPincodeById(@PathVariable UUID id) {
        PincodeResponseDTO pincode = pincodeService.getPincodeById(id);
        return ResponseEntity.ok(ApiResponse.success("Pincode fetched successfully", pincode));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pincode (delivery days & COD availability)")
    public ResponseEntity<ApiResponse<PincodeResponseDTO>> updatePincode(@PathVariable UUID id, @Valid @RequestBody UpdatePincodeRequestDTO request) {
        PincodeResponseDTO pincode = pincodeService.updatePincode(id, request);
        return ResponseEntity.ok(ApiResponse.success("Pincode updated successfully", pincode));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a serviceable pincode")
    public ResponseEntity<ApiResponse<Void>> deletePincode(@PathVariable UUID id) {
        pincodeService.deletePincode(id);
        return ResponseEntity.ok(ApiResponse.success("Pincode deleted successfully", null));
    }
}
