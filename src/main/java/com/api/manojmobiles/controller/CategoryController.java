package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.category.CategoryRequestDTO;
import com.api.manojmobiles.dto.category.CategoryResponseDTO;
import com.api.manojmobiles.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@Tag(name = "Category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;



    @GetMapping("/api/public/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getRootCategories() {
        List<CategoryResponseDTO> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(ApiResponse.success("Root categories fetched successfully", categories));
    }

    @GetMapping("/api/public/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable UUID id) {
        CategoryResponseDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category fetched successfully", category));
    }

    @GetMapping("/api/public/categories/{id}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getSubCategories(@PathVariable UUID id) {
        List<CategoryResponseDTO> subcategories = categoryService.getSubCategories(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategories fetched successfully", subcategories));
    }

    @GetMapping("/api/public/categories/slug/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponseDTO category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Category fetched successfully", category));
    }



    @PostMapping("/api/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(
            @Valid @RequestBody CategoryRequestDTO request) {
        CategoryResponseDTO created = categoryService.createCategory(request);
        return new ResponseEntity<>(ApiResponse.success("Category created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/api/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequestDTO request) {
        CategoryResponseDTO updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updated));
    }

    @DeleteMapping("/api/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}
