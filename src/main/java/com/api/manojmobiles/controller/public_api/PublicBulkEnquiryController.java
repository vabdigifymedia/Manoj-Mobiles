package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.enquiry.SubmitBulkEnquiryRequestDTO;
import com.api.manojmobiles.service.BulkEnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/bulk-enquiry")
@RequiredArgsConstructor
@Tag(name = "Public Bulk Enquiry APIs", description = "Endpoints for submitting bulk wholesale enquiries")
public class PublicBulkEnquiryController {

    private final BulkEnquiryService bulkEnquiryService;

    @Operation(summary = "Submit bulk enquiry", description = "Allows visitors to submit a bulk order enquiry for wholesale pricing")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitEnquiry(@Valid @RequestBody SubmitBulkEnquiryRequestDTO request) {
        bulkEnquiryService.submitEnquiry(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk enquiry submitted successfully. Our team will contact you soon.", null));
    }
}
