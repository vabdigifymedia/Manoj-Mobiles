package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.product.ImageResponseDTO;
import com.api.manojmobiles.dto.product.ProductListResponseDTO;
import com.api.manojmobiles.dto.product.ProductRequestDTO;
import com.api.manojmobiles.dto.product.ProductResponseDTO;
import com.api.manojmobiles.dto.product.ProductSpecificationRequestDTO;
import com.api.manojmobiles.dto.product.ProductSpecificationResponseDTO;
import com.api.manojmobiles.dto.product.ProductVariantRequestDTO;
import com.api.manojmobiles.dto.product.ProductVariantResponseDTO;
import com.api.manojmobiles.entity.Brand;
import com.api.manojmobiles.entity.Category;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductImage;
import com.api.manojmobiles.entity.ProductSpecification;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.enums.StockStatus;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.BrandRepository;
import com.api.manojmobiles.repository.CategoryRepository;
import com.api.manojmobiles.repository.ProductImageRepository;
import com.api.manojmobiles.repository.ProductRepository;
import com.api.manojmobiles.repository.ProductSpecificationRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import com.api.manojmobiles.repository.ProductHighlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.api.manojmobiles.dto.product.InventoryAdjustmentRequestDTO;
import com.api.manojmobiles.entity.InventoryLog;
import com.api.manojmobiles.repository.InventoryLogRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductSpecificationRepository specRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductHighlightRepository productHighlightRepository;



    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String uniqueSlug = "";
        if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
            uniqueSlug = request.getSlug().trim();
            if (productRepository.existsBySlugIgnoreCase(uniqueSlug)) {
                throw new com.api.manojmobiles.exception.BadRequestException("Slug '" + uniqueSlug + "' is already in use. Please choose a unique slug.");
            }
        } else {
            String baseSlug = request.getName().toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            if (baseSlug.isBlank()) {
                baseSlug = "product";
            }
            if (baseSlug.length() > 200) {
                baseSlug = baseSlug.substring(0, 200);
            }

            uniqueSlug = baseSlug;
            int count = 1;
            while (productRepository.existsBySlugIgnoreCase(uniqueSlug)) {
                uniqueSlug = baseSlug + "-" + count;
                count++;
                if (count > 50) {
                    uniqueSlug = baseSlug + "-" + System.currentTimeMillis();
                    break;
                }
            }
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(uniqueSlug)
                .description(request.getDescription())
                .brand(brand)
                .category(category)
                .warrantyMonths(request.getWarrantyMonths() != null ? request.getWarrantyMonths() : 12)
                .returnPolicyDays(request.getReturnPolicyDays() != null ? request.getReturnPolicyDays() : 7)
                .isReturnable(request.getIsReturnable() != null ? request.getIsReturnable() : true)
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: {} (id={})", saved.getName(), saved.getId());
        return mapToResponseDTO(saved);
    }

    @Cacheable(value = "products", key = "'product:' + #id")
    public ProductResponseDTO getProductById(UUID id) {
        log.info("Cache MISS for product:{} — fetching from database", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponseDTO(product);
    }

    public Page<ProductListResponseDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::mapToListDTO);
    }

    public ProductResponseDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return mapToResponseDTO(product);
    }

    public Page<ProductListResponseDTO> getProductsByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(this::mapToListDTO);
    }

    public Page<ProductListResponseDTO> searchProducts(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts(pageable);
        }
        return productRepository.searchProducts(query, pageable)
                .map(this::mapToListDTO);
    }

    public Page<com.api.manojmobiles.dto.product.ProductVariantResponseDTO> filterProducts(com.api.manojmobiles.dto.product.ProductFilterRequestDTO filter, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<ProductVariant> spec = com.api.manojmobiles.specification.ProductSpecification.getProductsByFilter(filter);
        return variantRepository.findAll(spec, pageable).map(this::mapVariantToDTO);
    }

    @Transactional
    @CacheEvict(value = "products", key = "'product:' + #id")
    public ProductResponseDTO updateProduct(UUID id, ProductRequestDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getWarrantyMonths() != null) product.setWarrantyMonths(request.getWarrantyMonths());
        if (request.getReturnPolicyDays() != null) product.setReturnPolicyDays(request.getReturnPolicyDays());
        if (request.getIsReturnable() != null) product.setIsReturnable(request.getIsReturnable());

        if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
            String newSlug = request.getSlug().trim();
            if (!newSlug.equalsIgnoreCase(product.getSlug())) {
                if (productRepository.existsBySlugIgnoreCase(newSlug)) {
                    throw new com.api.manojmobiles.exception.BadRequestException("Slug '" + newSlug + "' is already in use. Please choose a unique slug.");
                }
                product.setSlug(newSlug);
            }
        }

        if (request.getMetaTitle() != null) product.setMetaTitle(request.getMetaTitle());
        if (request.getMetaDescription() != null) product.setMetaDescription(request.getMetaDescription());
        if (request.getMetaKeywords() != null) product.setMetaKeywords(request.getMetaKeywords());

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            product.setBrand(brand);
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: {} (id={})", saved.getName(), saved.getId());
        return mapToResponseDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "products", key = "'product:' + #productId")
    public void updateProductStatus(UUID productId, com.api.manojmobiles.entity.enums.ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (status == com.api.manojmobiles.entity.enums.ProductStatus.ACTIVE) {
            // Validate all variant colors have images before publishing
            List<ProductVariant> variants = product.getVariants();
            if (variants == null || variants.isEmpty()) {
                throw new com.api.manojmobiles.exception.BadRequestException("Cannot publish product without any variants.");
            }
            
            java.util.Set<String> colors = variants.stream()
                .map(v -> v.getColor())
                .filter(c -> c != null)
                .collect(java.util.stream.Collectors.toSet());
                
            List<ProductImage> allImages = product.getImages();
            for (String color : colors) {
                boolean hasImage = allImages != null && allImages.stream().anyMatch(img -> color.equals(img.getColor()));
                if (!hasImage) {
                    throw new com.api.manojmobiles.exception.BadRequestException("Cannot publish product: Color group '" + color + "' has no images.");
                }
            }
        }
        product.setStatus(status);
        productRepository.save(product);
        log.info("Product status updated: id={} status={}", productId, status);
    }

    @Transactional
    @CacheEvict(value = "products", key = "'product:' + #id")
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Clean up all related child tables referencing product or its variants
        productRepository.deleteInventoryLogsByProductId(id);
        productRepository.deleteProductQuestionsByProductId(id);
        productRepository.deleteReviewsByProductId(id);
        productRepository.deleteWishlistByProductId(id);
        productRepository.deleteCartItemsByProductId(id);
        productRepository.deleteCompareListByProductId(id);
        productRepository.deleteOffersByProductId(id);
        productRepository.deleteRecentlyViewedByProductId(id);

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (ProductVariant variant : product.getVariants()) {
                if (variant.getImages() != null && !variant.getImages().isEmpty()) {
                    imageRepository.deleteAll(variant.getImages());
                }
                if (variant.getSpecifications() != null && !variant.getSpecifications().isEmpty()) {
                    specRepository.deleteAll(variant.getSpecifications());
                }
                variantRepository.delete(variant);
            }
        }

        productRepository.delete(product);
        log.info("Product deleted: id={}", id);
    }



    public ProductVariantResponseDTO getVariantById(UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
        return mapVariantToDTO(variant);
    }

    @Transactional
    public ProductVariantResponseDTO adjustInventory(UUID variantId, InventoryAdjustmentRequestDTO request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        int newQty = variant.getStockQty() + request.getChangeQty();
        if (newQty < 0) {
            throw new com.api.manojmobiles.exception.BadRequestException("Inventory cannot go below 0");
        }

        variant.setStockQty(newQty);
        variant.setStockStatus(determineStockStatus(newQty));

        variantRepository.save(variant);

        InventoryLog inventoryLog = InventoryLog.builder()
                .variant(variant)
                .changeQty(request.getChangeQty())
                .reason(request.getReason())
                .build();
        inventoryLogRepository.save(inventoryLog);

        log.info("Inventory adjusted for variant {}. Change: {}. New Qty: {}", variantId, request.getChangeQty(), newQty);
        return mapVariantToDTO(variant);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductVariantResponseDTO createVariant(ProductVariantRequestDTO request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (request.getSku() != null && variantRepository.existsBySku(request.getSku())) {
            throw new com.api.manojmobiles.exception.BadRequestException("Variant SKU '" + request.getSku() + "' already exists. Please enter a unique SKU.");
        }

        int discount = calculateDiscountPercent(request.getMrp(), request.getSellingPrice());
        StockStatus stockStatus = determineStockStatus(request.getStockQty());

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .variantName(request.getVariantName())
                .sku(request.getSku())
                .color(request.getColor())
                .mrp(request.getMrp())
                .sellingPrice(request.getSellingPrice())
                .discountPercent(discount)
                .gstPercent(request.getGstPercent() != null ? request.getGstPercent() : BigDecimal.ZERO)
                .stockQty(request.getStockQty())
                .stockStatus(stockStatus)
                .codAvailable(request.getCodAvailable() != null ? request.getCodAvailable() : true)
                .build();

        ProductVariant saved = variantRepository.save(variant);
        log.info("Variant created: {} (sku={}) for product {}", saved.getVariantName(), saved.getSku(), product.getId());
        return mapVariantToDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductVariantResponseDTO updateVariant(UUID variantId, ProductVariantRequestDTO request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
        if (request.getSku() != null) {
            if (!request.getSku().equalsIgnoreCase(variant.getSku()) && variantRepository.existsBySku(request.getSku())) {
                throw new com.api.manojmobiles.exception.BadRequestException("Variant SKU '" + request.getSku() + "' already exists. Please enter a unique SKU.");
            }
            variant.setSku(request.getSku());
        }
        if (request.getColor() != null) variant.setColor(request.getColor());
        if (request.getGstPercent() != null) variant.setGstPercent(request.getGstPercent());
        if (request.getCodAvailable() != null) variant.setCodAvailable(request.getCodAvailable());

        // Recalculate discount if price fields changed
        BigDecimal mrp = request.getMrp() != null ? request.getMrp() : variant.getMrp();
        BigDecimal sellingPrice = request.getSellingPrice() != null ? request.getSellingPrice() : variant.getSellingPrice();
        variant.setMrp(mrp);
        variant.setSellingPrice(sellingPrice);
        variant.setDiscountPercent(calculateDiscountPercent(mrp, sellingPrice));

        // Recalculate stock status if qty changed
        if (request.getStockQty() != null) {
            variant.setStockQty(request.getStockQty());
            variant.setStockStatus(determineStockStatus(request.getStockQty()));
        }

        ProductVariant saved = variantRepository.save(variant);
        log.info("Variant updated: id={}", variantId);
        return mapVariantToDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteVariant(UUID variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Variant not found with id: " + variantId);
        }

        // Clean up child tables referencing this variant
        productRepository.deleteInventoryLogsByVariantId(variantId);
        productRepository.deleteWishlistByVariantId(variantId);
        productRepository.deleteCartItemsByVariantId(variantId);
        productRepository.deleteCompareListByVariantId(variantId);
        productRepository.deleteOffersByVariantId(variantId);
        productRepository.deleteRecentlyViewedByVariantId(variantId);

        variantRepository.deleteById(variantId);
        log.info("Variant deleted: id={}", variantId);
    }



    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void addVariantImages(UUID variantId, List<String> imageUrls) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        String color = variant.getColor();
        UUID productId = variant.getProduct().getId();
        List<ProductImage> existingImages = imageRepository.findByProductIdAndColor(productId, color);
        imageRepository.deleteAll(existingImages);
        boolean hasExistingImages = false;


        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = ProductImage.builder()
                    .product(variant.getProduct())
                    .color(color)
                    .url(imageUrls.get(i))
                    .isPrimary(!hasExistingImages && i == 0) // first image of the color is primary
                    .build();
            imageRepository.save(image);
        }
        log.info("Added {} images to product {} color {}", imageUrls.size(), productId, color);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteImage(UUID imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        UUID productId = image.getProduct().getId();
        String color = image.getColor();
        boolean wasPrimary = image.getIsPrimary() != null ? image.getIsPrimary() : false;

        imageRepository.delete(image);
        log.info("Image deleted: id={}", imageId);

        if (wasPrimary) {
            List<ProductImage> remainingImages = imageRepository.findByProductIdAndColor(productId, color);
            if (!remainingImages.isEmpty()) {
                ProductImage newPrimary = remainingImages.get(0);
                newPrimary.setIsPrimary(true);
                imageRepository.save(newPrimary);
                log.info("Auto-reassigned primary image to {} for product {} color {}", newPrimary.getId(), productId, color);
            }
        }
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteVariantImageUrl(UUID variantId, String imageUrl) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
                
        List<ProductImage> images = imageRepository.findByProductIdAndColor(variant.getProduct().getId(), variant.getColor());
        images.stream()
                .filter(img -> img.getUrl().equals(imageUrl))
                .findFirst()
                .ifPresent(img -> {
                    deleteImage(img.getId());
                    log.info("Removed legacy image URL from product color group: {}", variant.getColor());
                });
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void setPrimaryImage(UUID imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        UUID productId = image.getProduct().getId();
        String color = image.getColor();

        List<ProductImage> allImages = imageRepository.findByProductIdAndColor(productId, color);
        for (ProductImage img : allImages) {
            img.setIsPrimary(img.getId().equals(imageId));
            imageRepository.save(img);
        }
        log.info("Primary image set to {} for product {} color {}", imageId, productId, color);
    }



    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void addVariantSpecifications(UUID variantId, List<ProductSpecificationRequestDTO> specs) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        variant.getSpecifications().clear();

        for (ProductSpecificationRequestDTO spec : specs) {
            ProductSpecification entity = ProductSpecification.builder()
                    .variant(variant)
                    .specGroup(spec.getSpecGroup())
                    .specKey(spec.getSpecKey())
                    .specValue(spec.getSpecValue())
                    .build();
            variant.getSpecifications().add(entity);
        }
        variantRepository.save(variant);
        log.info("Added {} specifications to variant {}", specs.size(), variantId);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteSpecification(UUID specId) {
        if (!specRepository.existsById(specId)) {
            throw new ResourceNotFoundException("Specification not found with id: " + specId);
        }
        specRepository.deleteById(specId);
        log.info("Specification deleted: id={}", specId);
    }



    private int calculateDiscountPercent(BigDecimal mrp, BigDecimal sellingPrice) {
        if (mrp == null || sellingPrice == null || mrp.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        BigDecimal discount = mrp.subtract(sellingPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(mrp, 0, RoundingMode.HALF_UP);
        return Math.max(discount.intValue(), 0);
    }

    private StockStatus determineStockStatus(int qty) {
        if (qty == 0) return StockStatus.OUT_OF_STOCK;
        if (qty <= 5) return StockStatus.LIMITED_STOCK;
        return StockStatus.IN_STOCK;
    }



    private ProductResponseDTO mapToResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .description(product.getDescription())
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .warrantyMonths(product.getWarrantyMonths())
                .returnPolicyDays(product.getReturnPolicyDays())
                .isReturnable(product.getIsReturnable())
                .avgRating(product.getAvgRating())
                .totalReviews(product.getTotalReviews())
                .slug(product.getSlug())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .metaKeywords(product.getMetaKeywords())
                .variants(mapVariants(product.getVariants()))
                .highlights(mapHighlights(product.getHighlights()))
                .build();
    }

    private List<com.api.manojmobiles.dto.product.HighlightResponseDTO> mapHighlights(List<com.api.manojmobiles.entity.ProductHighlight> highlights) {
        if (highlights == null) return java.util.Collections.emptyList();
        return highlights.stream()
                .sorted(java.util.Comparator.comparingInt(h -> h.getDisplayOrder() != null ? h.getDisplayOrder() : 0))
                .map(h -> com.api.manojmobiles.dto.product.HighlightResponseDTO.builder()
                        .id(h.getId())
                        .iconName(h.getIconName())
                        .text(h.getText())
                        .displayOrder(h.getDisplayOrder())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private List<ProductVariantResponseDTO> mapVariants(List<ProductVariant> variants) {
        if (variants == null) return Collections.emptyList();
        return variants.stream()
                .map(this::mapVariantToDTO)
                .collect(Collectors.toList());
    }

    private ProductVariantResponseDTO mapVariantToDTO(ProductVariant v) {
        return ProductVariantResponseDTO.builder()
                .id(v.getId())
                .variantName(v.getVariantName())
                .sku(v.getSku())
                .color(v.getColor())
                .mrp(v.getMrp())
                .sellingPrice(v.getSellingPrice())
                .discountPercent(v.getDiscountPercent())
                .gstPercent(v.getGstPercent())
                .stockQty(v.getStockQty())
                .stockStatus(v.getStockStatus())
                .codAvailable(v.getCodAvailable())
                .imageUrls(v.getImages() != null
                        ? v.getImages().stream().map(img -> img.getUrl()).collect(Collectors.toList())
                        : Collections.emptyList())
                .images(v.getImages() != null
                        ? v.getImages().stream().map(img -> ImageResponseDTO.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .isPrimary(img.getIsPrimary())
                                .build()).collect(Collectors.toList())
                        : Collections.emptyList())
                .specifications(v.getSpecifications() != null
                        ? v.getSpecifications().stream().map(spec -> ProductSpecificationResponseDTO.builder()
                                .specGroup(spec.getSpecGroup())
                                .specKey(spec.getSpecKey())
                                .specValue(spec.getSpecValue())
                                .build())
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    private ProductListResponseDTO mapToListDTO(Product product) {
        BigDecimal startingPrice = null;
        String primaryImageUrl = null;

        List<ProductVariant> variants = product.getVariants();
        if (variants != null && !variants.isEmpty()) {
            startingPrice = variants.stream()
                    .filter(v -> v != null && v.getSellingPrice() != null)
                    .map(v -> v.getSellingPrice())
                    .min(java.util.Comparator.naturalOrder())
                    .orElse(null);


            // Use the primary image of the first variant as the card thumbnail
            for (ProductVariant v : variants) {
                if (v.getImages() != null && !v.getImages().isEmpty()) {
                    primaryImageUrl = v.getImages().get(0).getUrl();
                    break;
                }
            }
        }

        return ProductListResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .slug(product.getSlug())
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .startingPrice(startingPrice)
                .primaryImageUrl(primaryImageUrl)
                .avgRating(product.getAvgRating())
                .totalReviews(product.getTotalReviews())
                .build();
    }

    @Transactional
    public com.api.manojmobiles.dto.product.HighlightResponseDTO addHighlight(UUID productId, com.api.manojmobiles.dto.product.CreateHighlightRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        if (productHighlightRepository.countByProductId(productId) >= 6) {
            throw new com.api.manojmobiles.exception.BadRequestException("Maximum of 6 highlights allowed per product.");
        }
        com.api.manojmobiles.entity.ProductHighlight highlight = com.api.manojmobiles.entity.ProductHighlight.builder()
                .product(product)
                .iconName(request.getIconName())
                .text(request.getText())
                .displayOrder(request.getDisplayOrder())
                .build();
        return mapHighlights(java.util.Collections.singletonList(productHighlightRepository.save(highlight))).get(0);
    }

    @Transactional
    public com.api.manojmobiles.dto.product.HighlightResponseDTO updateHighlight(UUID highlightId, com.api.manojmobiles.dto.product.UpdateHighlightRequestDTO request) {
        com.api.manojmobiles.entity.ProductHighlight highlight = productHighlightRepository.findById(highlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Highlight not found with id: " + highlightId));
        if (request.getIconName() != null) highlight.setIconName(request.getIconName());
        if (request.getText() != null) highlight.setText(request.getText());
        return mapHighlights(java.util.Collections.singletonList(productHighlightRepository.save(highlight))).get(0);
    }

    @Transactional
    public void deleteHighlight(UUID highlightId) {
        if (!productHighlightRepository.existsById(highlightId)) {
            throw new ResourceNotFoundException("Highlight not found with id: " + highlightId);
        }
        productHighlightRepository.deleteById(highlightId);
    }

    @Transactional
    public void reorderHighlights(UUID productId, List<UUID> orderedHighlightIds) {
        List<com.api.manojmobiles.entity.ProductHighlight> highlights = productHighlightRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        for (int i = 0; i < orderedHighlightIds.size(); i++) {
             UUID id = orderedHighlightIds.get(i);
             for(com.api.manojmobiles.entity.ProductHighlight h : highlights) {
                 if(h.getId().equals(id)) {
                     h.setDisplayOrder(i);
                 }
             }
        }
        productHighlightRepository.saveAll(highlights);
    }
}
