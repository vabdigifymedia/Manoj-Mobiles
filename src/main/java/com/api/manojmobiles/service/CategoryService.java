package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.category.CategoryRequestDTO;
import com.api.manojmobiles.dto.category.CategoryResponseDTO;
import com.api.manojmobiles.entity.Category;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CategoryRepository;
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
 * Category service with Redis caching.
 *
 * Cache strategy:
 * - getCategoryById: @Cacheable — cache hit avoids DB query
 * - updateCategory: @CachePut — updates both DB and cache
 * - deleteCategory: @CacheEvict — removes from cache on delete
 * - getRootCategories / getSubCategories: NOT cached — tree queries are dynamic
 *
 * Redis key pattern: categories::category:{id}
 * TTL: configured via app.redis.cache.category-ttl (default 6h)
 * Database remains the source of truth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Get a single category by ID.
     * Cached in Redis under key "categories::category:{id}".
     * On cache miss, fetches from DB and populates cache.
     */
    @Cacheable(value = "categories", key = "'category:' + #id")
    public CategoryResponseDTO getCategoryById(UUID id) {
        log.info("Cache MISS for category:{} — fetching from database", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponseDTO(category);
    }

    /**
     * Get all root categories (no parent). NOT cached — tree queries are dynamic.
     */
    public List<CategoryResponseDTO> getRootCategories() {
        return categoryRepository.findByParentIsNull().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get subcategories of a given parent. NOT cached.
     */
    public List<CategoryResponseDTO> getSubCategories(UUID parentId) {
        return categoryRepository.findByParentId(parentId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a category by slug. NOT cached — slug lookups are less common.
     */
    public CategoryResponseDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return mapToResponseDTO(category);
    }

    /**
     * Create a new category. No caching annotation needed — fresh entity.
     */
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        Category category = Category.builder()
                .name(request.getName())
                .slug(generateSlug(request.getName()))
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .build();

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent category not found with id: " + request.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Created category:{}", saved.getId());
        return mapToResponseDTO(saved);
    }

    /**
     * Update a category. Uses @CachePut to refresh the cache entry.
     */
    @Transactional
    @CachePut(value = "categories", key = "'category:' + #id")
    public CategoryResponseDTO updateCategory(UUID id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setImageUrl(request.getImageUrl());
        category.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent category not found with id: " + request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        log.info("Cache PUT for category:{} — cache updated after DB write", id);
        return mapToResponseDTO(saved);
    }

    /**
     * Delete a category. @CacheEvict removes the cached entry.
     */
    @Transactional
    @CacheEvict(value = "categories", key = "'category:' + #id")
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Cache EVICT for category:{} — removed after deletion", id);
    }

    // Mapping

    private CategoryResponseDTO mapToResponseDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .imageUrl(category.getImageUrl())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(category.getChildren() != null
                        ? category.getChildren().stream()
                            .map(this::mapToResponseDTO)
                            .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
