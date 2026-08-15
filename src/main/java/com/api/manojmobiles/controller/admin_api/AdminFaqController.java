package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.request.FaqRequestDTO;
import com.api.manojmobiles.dto.request.ReorderRequestDTO;
import com.api.manojmobiles.dto.response.FaqResponseDTO;
import com.api.manojmobiles.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin FAQ Management", description = "Endpoints for managing FAQs")
public class AdminFaqController {

    private final FaqService faqService;

    @GetMapping
    @Operation(summary = "Get all FAQs", description = "Fetches all FAQs including inactive ones")
    public ResponseEntity<ApiResponse<List<FaqResponseDTO>>> getAllFaqs() {
        List<FaqResponseDTO> faqs = faqService.getAllFaqs();
        return ResponseEntity.ok(ApiResponse.success("FAQs retrieved successfully", faqs));
    }

    @PostMapping
    @Operation(summary = "Create FAQ", description = "Creates a new FAQ")
    public ResponseEntity<ApiResponse<FaqResponseDTO>> createFaq(@Valid @RequestBody FaqRequestDTO dto) {
        FaqResponseDTO created = faqService.createFaq(dto);
        return ResponseEntity.ok(ApiResponse.success("FAQ created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update FAQ", description = "Updates an existing FAQ")
    public ResponseEntity<ApiResponse<FaqResponseDTO>> updateFaq(
            @PathVariable UUID id, @Valid @RequestBody FaqRequestDTO dto) {
        FaqResponseDTO updated = faqService.updateFaq(id, dto);
        return ResponseEntity.ok(ApiResponse.success("FAQ updated successfully", updated));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update FAQ status", description = "Toggles FAQ active status")
    public ResponseEntity<ApiResponse<Void>> updateFaqStatus(
            @PathVariable UUID id, @RequestParam boolean active) {
        faqService.updateFaqStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("FAQ status updated successfully", null));
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder FAQs", description = "Updates the display order of FAQs")
    public ResponseEntity<ApiResponse<Void>> reorderFaqs(@Valid @RequestBody ReorderRequestDTO dto) {
        faqService.reorderFaqs(dto);
        return ResponseEntity.ok(ApiResponse.success("FAQs reordered successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete FAQ", description = "Deletes an FAQ")
    public ResponseEntity<ApiResponse<Void>> deleteFaq(@PathVariable UUID id) {
        faqService.deleteFaq(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ deleted successfully", null));
    }
}
