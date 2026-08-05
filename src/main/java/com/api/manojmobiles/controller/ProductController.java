package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.product.ProductRequestDTO;
import com.api.manojmobiles.dto.product.ProductResponseDTO;
import com.api.manojmobiles.dto.product.ProductSpecificationRequestDTO;
import com.api.manojmobiles.dto.product.ProductVariantRequestDTO;
import com.api.manojmobiles.dto.product.ProductVariantResponseDTO;
import com.api.manojmobiles.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ======================== Public (read) endpoints ========================

    @GetMapping("/api/public/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getAllProducts() {
        List<ProductResponseDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    @GetMapping("/api/public/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(@PathVariable UUID id) {
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", product));
    }

    @GetMapping("/api/public/products/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductBySlug(@PathVariable String slug) {
        ProductResponseDTO product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", product));
    }

    @GetMapping("/api/public/products/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getProductsByCategory(@PathVariable UUID categoryId) {
        List<ProductResponseDTO> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    // ======================== Admin - Product CRUD ========================

    @PostMapping("/api/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO created = productService.createProduct(request);
        return new ResponseEntity<>(ApiResponse.success("Product created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/api/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @DeleteMapping("/api/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    // ======================== Admin - Variant CRUD ========================

    @PostMapping("/api/products/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponseDTO>> createVariant(
            @Valid @RequestBody ProductVariantRequestDTO request) {
        ProductVariantResponseDTO created = productService.createVariant(request);
        return new ResponseEntity<>(ApiResponse.success("Variant created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/api/products/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponseDTO>> updateVariant(
            @PathVariable UUID id,
            @Valid @RequestBody ProductVariantRequestDTO request) {
        ProductVariantResponseDTO updated = productService.updateVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Variant updated successfully", updated));
    }

    @DeleteMapping("/api/products/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable UUID id) {
        productService.deleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Variant deleted successfully", null));
    }

    // ======================== Admin - Image CRUD ========================

    @PostMapping("/api/products/variants/{variantId}/images")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addVariantImages(
            @PathVariable UUID variantId,
            @RequestBody List<String> imageUrls) {
        productService.addVariantImages(variantId, imageUrls);
        return new ResponseEntity<>(ApiResponse.success("Images added successfully", null), HttpStatus.CREATED);
    }

    @DeleteMapping("/api/products/images/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable UUID id) {
        productService.deleteImage(id);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }

    // ======================== Admin - Specification CRUD ========================

    @PostMapping("/api/products/variants/{variantId}/specifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addVariantSpecifications(
            @PathVariable UUID variantId,
            @Valid @RequestBody List<ProductSpecificationRequestDTO> specs) {
        productService.addVariantSpecifications(variantId, specs);
        return new ResponseEntity<>(ApiResponse.success("Specifications added successfully", null), HttpStatus.CREATED);
    }

    @DeleteMapping("/api/products/specifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSpecification(@PathVariable UUID id) {
        productService.deleteSpecification(id);
        return ResponseEntity.ok(ApiResponse.success("Specification deleted successfully", null));
    }
}
