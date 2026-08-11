package com.api.manojmobiles.controller;

import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.product.ProductListResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.api.manojmobiles.dto.product.InventoryAdjustmentRequestDTO;

import org.springdoc.core.annotations.ParameterObject;

@RestController
@Tag(name = "Product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;



    @GetMapping("/api/public/products")
    public ResponseEntity<ApiResponse<Page<ProductListResponseDTO>>> getAllProducts(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductListResponseDTO> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    @GetMapping("/api/public/products/search")
    public ResponseEntity<ApiResponse<Page<ProductListResponseDTO>>> searchProducts(
            @RequestParam("q") String query,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductListResponseDTO> products = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products searched successfully", products));
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
    public ResponseEntity<ApiResponse<Page<ProductListResponseDTO>>> getProductsByCategory(
            @PathVariable UUID categoryId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductListResponseDTO> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    @PostMapping("/api/public/products/filter")
    public ResponseEntity<ApiResponse<Page<ProductVariantResponseDTO>>> filterProducts(
            @RequestBody com.api.manojmobiles.dto.product.ProductFilterRequestDTO filterRequest,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductVariantResponseDTO> variants = productService.filterProducts(filterRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products filtered successfully", variants));
    }

    @GetMapping("/api/products/variants/{id}")
    public ResponseEntity<ApiResponse<ProductVariantResponseDTO>> getVariantById(@PathVariable UUID id) {
        ProductVariantResponseDTO variant = productService.getVariantById(id);
        return ResponseEntity.ok(ApiResponse.success("Variant fetched successfully", variant));
    }



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

    @PutMapping("/api/products/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProductStatus(
            @PathVariable UUID id,
            @RequestParam com.api.manojmobiles.entity.enums.ProductStatus status) {
        productService.updateProductStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Product status updated successfully", null));
    }

    @DeleteMapping("/api/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }



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

    @PostMapping("/api/products/variants/{variantId}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponseDTO>> adjustInventory(
            @PathVariable UUID variantId,
            @Valid @RequestBody InventoryAdjustmentRequestDTO request) {
        ProductVariantResponseDTO updated = productService.adjustInventory(variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Inventory adjusted successfully", updated));
    }



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

    @PutMapping("/api/products/images/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(@PathVariable UUID imageId) {
        productService.setPrimaryImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Primary image set successfully", null));
    }



    @PostMapping("/api/products/variants/{variantId}/specifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addVariantSpecifications(
            @PathVariable UUID variantId,
            @RequestBody List<@Valid ProductSpecificationRequestDTO> specs) {
        productService.addVariantSpecifications(variantId, specs);
        return new ResponseEntity<>(ApiResponse.success("Specifications added successfully", null), HttpStatus.CREATED);
    }

    @DeleteMapping("/api/products/specifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSpecification(@PathVariable UUID id) {
        productService.deleteSpecification(id);
        return ResponseEntity.ok(ApiResponse.success("Specification deleted successfully", null));
    }

    // --- Highlights API ---
    @PostMapping("/api/products/{productId}/highlights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.api.manojmobiles.dto.product.HighlightResponseDTO>> addHighlight(
            @PathVariable UUID productId,
            @Valid @RequestBody com.api.manojmobiles.dto.product.CreateHighlightRequestDTO request) {
        com.api.manojmobiles.dto.product.HighlightResponseDTO highlight = productService.addHighlight(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Highlight added successfully", highlight));
    }

    @PutMapping("/api/products/highlights/{highlightId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.api.manojmobiles.dto.product.HighlightResponseDTO>> updateHighlight(
            @PathVariable UUID highlightId,
            @Valid @RequestBody com.api.manojmobiles.dto.product.UpdateHighlightRequestDTO request) {
        com.api.manojmobiles.dto.product.HighlightResponseDTO highlight = productService.updateHighlight(highlightId, request);
        return ResponseEntity.ok(ApiResponse.success("Highlight updated successfully", highlight));
    }

    @DeleteMapping("/api/products/highlights/{highlightId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteHighlight(@PathVariable UUID highlightId) {
        productService.deleteHighlight(highlightId);
        return ResponseEntity.ok(ApiResponse.success("Highlight deleted successfully", null));
    }

    @PutMapping("/api/products/{productId}/highlights/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorderHighlights(
            @PathVariable UUID productId,
            @RequestBody List<UUID> orderedHighlightIds) {
        productService.reorderHighlights(productId, orderedHighlightIds);
        return ResponseEntity.ok(ApiResponse.success("Highlights reordered successfully", null));
    }

    @DeleteMapping("/api/products/variants/{variantId}/image-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVariantImageUrl(
            @PathVariable UUID variantId,
            @RequestParam("url") String imageUrl) {
        productService.deleteVariantImageUrl(variantId, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Legacy image URL deleted successfully", null));
    }
}
