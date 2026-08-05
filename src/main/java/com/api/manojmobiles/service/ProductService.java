package com.api.manojmobiles.service;

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

    // ======================== Product CRUD ========================

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .brand(brand)
                .category(category)
                .warrantyMonths(request.getWarrantyMonths())
                .returnPolicyDays(request.getReturnPolicyDays())
                .isReturnable(request.getIsReturnable())
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

    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return mapToResponseDTO(product);
    }

    public List<ProductResponseDTO> getProductsByCategory(UUID categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
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
    @CacheEvict(value = "products", key = "'product:' + #id")
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted: id={}", id);
    }

    // ======================== Variant CRUD ========================

    @Transactional
    @CacheEvict(value = "products", key = "'product:' + #request.productId")
    public ProductVariantResponseDTO createVariant(ProductVariantRequestDTO request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        int discount = calculateDiscountPercent(request.getMrp(), request.getSellingPrice());
        StockStatus stockStatus = determineStockStatus(request.getStockQty());

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .variantName(request.getVariantName())
                .sku(request.getSku())
                .mrp(request.getMrp())
                .sellingPrice(request.getSellingPrice())
                .discountPercent(discount)
                .gstPercent(request.getGstPercent())
                .stockQty(request.getStockQty())
                .stockStatus(stockStatus)
                .codAvailable(request.getCodAvailable())
                .build();

        ProductVariant saved = variantRepository.save(variant);
        log.info("Variant created: {} (sku={}) for product {}", saved.getVariantName(), saved.getSku(), product.getId());
        return mapVariantToDTO(saved);
    }

    @Transactional
    public ProductVariantResponseDTO updateVariant(UUID variantId, ProductVariantRequestDTO request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
        if (request.getSku() != null) variant.setSku(request.getSku());
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
    public void deleteVariant(UUID variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Variant not found with id: " + variantId);
        }
        variantRepository.deleteById(variantId);
        log.info("Variant deleted: id={}", variantId);
    }

    // ======================== Image CRUD ========================

    @Transactional
    public void addVariantImages(UUID variantId, List<String> imageUrls) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        boolean hasExistingImages = imageRepository.findByVariantId(variantId).size() > 0;

        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = ProductImage.builder()
                    .variant(variant)
                    .url(imageUrls.get(i))
                    .isPrimary(!hasExistingImages && i == 0) // first image of the variant is primary
                    .build();
            imageRepository.save(image);
        }
        log.info("Added {} images to variant {}", imageUrls.size(), variantId);
    }

    @Transactional
    public void deleteImage(UUID imageId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ResourceNotFoundException("Image not found with id: " + imageId);
        }
        imageRepository.deleteById(imageId);
        log.info("Image deleted: id={}", imageId);
    }

    // ======================== Specification CRUD ========================

    @Transactional
    public void addVariantSpecifications(UUID variantId, List<ProductSpecificationRequestDTO> specs) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        for (ProductSpecificationRequestDTO spec : specs) {
            ProductSpecification entity = ProductSpecification.builder()
                    .variant(variant)
                    .specGroup(spec.getSpecGroup())
                    .specKey(spec.getSpecKey())
                    .specValue(spec.getSpecValue())
                    .build();
            specRepository.save(entity);
        }
        log.info("Added {} specifications to variant {}", specs.size(), variantId);
    }

    @Transactional
    public void deleteSpecification(UUID specId) {
        if (!specRepository.existsById(specId)) {
            throw new ResourceNotFoundException("Specification not found with id: " + specId);
        }
        specRepository.deleteById(specId);
        log.info("Specification deleted: id={}", specId);
    }

    // ======================== Helper Methods ========================

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

    // ======================== Mapping ========================

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
                .variants(mapVariants(product.getVariants()))
                .build();
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
                .mrp(v.getMrp())
                .sellingPrice(v.getSellingPrice())
                .discountPercent(v.getDiscountPercent())
                .gstPercent(v.getGstPercent())
                .stockQty(v.getStockQty())
                .stockStatus(v.getStockStatus())
                .codAvailable(v.getCodAvailable())
                .imageUrls(v.getImages() != null
                        ? v.getImages().stream().map(ProductImage::getUrl).collect(Collectors.toList())
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
}
