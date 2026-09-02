package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.enquiry.BulkEnquiryResponseDTO;
import com.api.manojmobiles.dto.enquiry.UpdateEnquiryStatusRequestDTO;
import com.api.manojmobiles.entity.enums.EnquiryStatus;
import com.api.manojmobiles.service.BulkEnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bulk-enquiry")
@RequiredArgsConstructor
@Tag(name = "Admin Bulk Enquiry APIs", description = "Endpoints for admins to manage bulk enquiries")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBulkEnquiryController {

    private final BulkEnquiryService bulkEnquiryService;

    @Operation(summary = "Get all bulk enquiries", description = "Fetches a paginated list of all bulk enquiries, optionally filtered by status")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BulkEnquiryResponseDTO>>> getAllEnquiries(
            @RequestParam(required = false) EnquiryStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<BulkEnquiryResponseDTO> page = bulkEnquiryService.getAllEnquiries(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Enquiries fetched successfully", page));
    }

    @Operation(summary = "Get single enquiry details", description = "Fetches complete details of a specific bulk enquiry")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BulkEnquiryResponseDTO>> getEnquiryById(@PathVariable UUID id) {
        BulkEnquiryResponseDTO enquiry = bulkEnquiryService.getEnquiryById(id);
        return ResponseEntity.ok(ApiResponse.success("Enquiry fetched successfully", enquiry));
    }

    @Operation(summary = "Update enquiry status", description = "Updates the status of a bulk enquiry (e.g., PENDING to RESOLVED)")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BulkEnquiryResponseDTO>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEnquiryStatusRequestDTO request) {
        BulkEnquiryResponseDTO updated = bulkEnquiryService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Enquiry status updated successfully", updated));
    }
}
