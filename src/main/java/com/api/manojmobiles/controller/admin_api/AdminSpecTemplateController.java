package com.api.manojmobiles.controller.admin_api;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.spectemplate.SpecTemplateRequestDTO;
import com.api.manojmobiles.dto.spectemplate.SpecTemplateResponseDTO;
import com.api.manojmobiles.service.SpecTemplateService;
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
@RequestMapping("/api/admin/spec-templates")
@RequiredArgsConstructor
@Tag(name = "Admin Spec Templates", description = "Admin APIs for managing specification templates")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpecTemplateController {

    private final SpecTemplateService specTemplateService;

    @Operation(summary = "Get all specification templates")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecTemplateResponseDTO>>> getAllSpecTemplates() {
        return ResponseEntity.ok(ApiResponse.success("Templates fetched successfully", specTemplateService.getAllSpecTemplates()));
    }

    @Operation(summary = "Get specification template by category ID")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<SpecTemplateResponseDTO>> getSpecTemplateByCategoryId(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success("Template fetched successfully", specTemplateService.getSpecTemplateByCategoryId(categoryId)));
    }

    @Operation(summary = "Create or update a specification template")
    @PostMapping
    public ResponseEntity<ApiResponse<SpecTemplateResponseDTO>> saveSpecTemplate(@Valid @RequestBody SpecTemplateRequestDTO requestDTO) {
        SpecTemplateResponseDTO response = specTemplateService.saveSpecTemplate(requestDTO);
        return new ResponseEntity<>(ApiResponse.success("Template saved successfully", response), HttpStatus.OK);
    }

    @Operation(summary = "Delete a specification template")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSpecTemplate(@PathVariable UUID id) {
        specTemplateService.deleteSpecTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
    }
}
