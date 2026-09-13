package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.pincode.*;
import com.api.manojmobiles.service.PincodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Operation(summary = "Get all serviceable pincodes with pagination, city filtering, and search")
    public ResponseEntity<ApiResponse<Page<PincodeResponseDTO>>> getAllPincodes(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "pincode"));
        Page<PincodeResponseDTO> pincodes = pincodeService.getAllPincodes(cityId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Pincodes fetched successfully", pincodes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pincode by ID")
    public ResponseEntity<ApiResponse<PincodeResponseDTO>> getPincodeById(@PathVariable UUID id) {
        PincodeResponseDTO pincode = pincodeService.getPincodeById(id);
        return ResponseEntity.ok(ApiResponse.success("Pincode fetched successfully", pincode));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pincode (pincode string, city, delivery days, COD, active status)")
    public ResponseEntity<ApiResponse<PincodeResponseDTO>> updatePincode(@PathVariable UUID id, @Valid @RequestBody UpdatePincodeRequestDTO request) {
        PincodeResponseDTO pincode = pincodeService.updatePincode(id, request);
        return ResponseEntity.ok(ApiResponse.success("Pincode updated successfully", pincode));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle pincode active status (soft enable / disable)")
    public ResponseEntity<ApiResponse<PincodeResponseDTO>> togglePincodeStatus(@PathVariable UUID id) {
        PincodeResponseDTO pincode = pincodeService.togglePincodeStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Pincode status toggled successfully", pincode));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a serviceable pincode")
    public ResponseEntity<ApiResponse<Void>> deletePincode(@PathVariable UUID id) {
        pincodeService.deletePincode(id);
        return ResponseEntity.ok(ApiResponse.success("Pincode deleted successfully", null));
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Bulk upload pincodes via JSON", description = "Add hundreds of pincodes to a city at once using a list of strings or DTO items")
    public ResponseEntity<ApiResponse<BulkPincodeResponseDTO>> bulkUploadJson(@Valid @RequestBody BulkPincodeUploadRequestDTO request) {
        BulkPincodeResponseDTO response = pincodeService.bulkUploadPincodes(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk pincode upload processed", response));
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk upload pincodes via CSV/Text file", description = "Upload a CSV file containing pincodes to add to a city")
    public ResponseEntity<ApiResponse<BulkPincodeResponseDTO>> bulkUploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("cityId") UUID cityId,
            @RequestParam(value = "estimatedDeliveryDays", required = false) Integer estimatedDeliveryDays,
            @RequestParam(value = "codAvailable", required = false) Boolean codAvailable) {
        BulkPincodeResponseDTO response = pincodeService.bulkUploadPincodesFromCsv(file, cityId, estimatedDeliveryDays, codAvailable);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk pincode CSV upload processed", response));
    }
}
