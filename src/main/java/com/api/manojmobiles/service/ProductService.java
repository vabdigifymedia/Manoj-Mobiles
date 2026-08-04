package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.product.ProductResponseDTO;
import com.api.manojmobiles.dto.product.ProductSpecificationResponseDTO;
import com.api.manojmobiles.dto.product.ProductVariantResponseDTO;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product service with Redis caching.
 *
 * Cache strategy:
 * - getProductById: @Cacheable — cache hit avoids DB query
 * - updateProduct: @CachePut — updates both DB and cache
 * - deleteProduct: @CacheEvict — removes from cache on delete
 * - getAllProducts: NOT cached — list queries change too frequently
 *
 * Redis key pattern: products::product:{id}
 * TTL: configured via app.redis.cache.product-ttl (default 1h)
 * Database remains the source of truth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Get a single product by ID.
     * Cached in Redis under key "products::product:{id}".
     * On cache miss, fetches from DB and populates cache.
     */
    @Cacheable(value = "products", key = "'product:' + #id")
    public ProductResponseDTO getProductById(UUID id) {
        log.info("Cache MISS for product:{} — fetching from database", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponseDTO(product);
    }

    /**
     * Get all products. NOT cached — list queries are too dynamic for cache.
     */
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a product by slug. NOT cached — slug lookups are less common.
     */
    public ProductResponseDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return mapToResponseDTO(product);
    }

    /**
     * Get products by category. NOT cached — filtered queries change frequently.
     */
    public List<ProductResponseDTO> getProductsByCategory(UUID categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update a product. Uses @CachePut to refresh the cache entry after DB update.
     * The cache key matches getProductById so the cached version stays in sync.
     */
    @Transactional
    @CachePut(value = "products", key = "'product:' + #id")
    public ProductResponseDTO updateProduct(UUID id, Product updatedFields) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (updatedFields.getName() != null) product.setName(updatedFields.getName());
        if (updatedFields.getDescription() != null) product.setDescription(updatedFields.getDescription());
        if (updatedFields.getWarrantyMonths() != null) product.setWarrantyMonths(updatedFields.getWarrantyMonths());
        if (updatedFields.getReturnPolicyDays() != null) product.setReturnPolicyDays(updatedFields.getReturnPolicyDays());
        if (updatedFields.getIsReturnable() != null) product.setIsReturnable(updatedFields.getIsReturnable());

        Product saved = productRepository.save(product);
        log.info("Cache PUT for product:{} — cache updated after DB write", id);
        return mapToResponseDTO(saved);
    }

    /**
     * Delete a product. @CacheEvict removes the cached entry.
     */
    @Transactional
    @CacheEvict(value = "products", key = "'product:' + #id")
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Cache EVICT for product:{} — removed after deletion", id);
    }

    // ─── Mapping ──────────────────────────────────────────

    private ProductResponseDTO mapToResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .avgRating(product.getAvgRating())
                .totalReviews(product.getTotalReviews())
                .slug(product.getSlug())
                .variants(mapVariants(product.getVariants()))
                .build();
    }

    private List<ProductVariantResponseDTO> mapVariants(List<ProductVariant> variants) {
        if (variants == null) return Collections.emptyList();
        return variants.stream().map(v -> ProductVariantResponseDTO.builder()
                .id(v.getId())
                .variantName(v.getVariantName())
                .sku(v.getSku())
                .mrp(v.getMrp())
                .sellingPrice(v.getSellingPrice())
                .discountPercent(v.getDiscountPercent())
                .stockStatus(v.getStockStatus())
                .imageUrls(v.getImages() != null
                        ? v.getImages().stream().map(img -> img.getUrl()).collect(Collectors.toList())
                        : Collections.emptyList())
                .specifications(v.getSpecifications() != null
                        ? v.getSpecifications().stream().map(spec -> ProductSpecificationResponseDTO.builder()
                                .specGroup(spec.getSpecGroup())
                                .specKey(spec.getSpecKey())
                                .specValue(spec.getSpecValue())
                                .build())
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build())
        .collect(Collectors.toList());
    }
}
