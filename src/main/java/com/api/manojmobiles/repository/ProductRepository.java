package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);
    boolean existsBySlugIgnoreCase(String slug);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByBrandId(UUID brandId, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:status IS NULL OR p.status = :status)")
    Page<Product> searchProducts(@Param("query") String query, @Param("status") ProductStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Product p SET p.brand = null WHERE p.brand.id = :brandId")
    void unlinkBrandFromProducts(@Param("brandId") UUID brandId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Product p SET p.category = null WHERE p.category.id = :categoryId")
    void unlinkCategoryFromProducts(@Param("categoryId") UUID categoryId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM inventory_log WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)", nativeQuery = true)
    void deleteInventoryLogsByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM product_question WHERE product_id = :productId", nativeQuery = true)
    void deleteProductQuestionsByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM review WHERE product_id = :productId", nativeQuery = true)
    void deleteReviewsByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM wishlist WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)", nativeQuery = true)
    void deleteWishlistByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM cart_item WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)", nativeQuery = true)
    void deleteCartItemsByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM compare_list WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)", nativeQuery = true)
    void deleteCompareListByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM offer WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)", nativeQuery = true)
    void deleteOffersByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM recently_viewed WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)", nativeQuery = true)
    void deleteRecentlyViewedByProductId(@Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM inventory_log WHERE variant_id = :variantId", nativeQuery = true)
    void deleteInventoryLogsByVariantId(@Param("variantId") UUID variantId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM wishlist WHERE variant_id = :variantId", nativeQuery = true)
    void deleteWishlistByVariantId(@Param("variantId") UUID variantId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM cart_item WHERE variant_id = :variantId", nativeQuery = true)
    void deleteCartItemsByVariantId(@Param("variantId") UUID variantId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM compare_list WHERE variant_id = :variantId", nativeQuery = true)
    void deleteCompareListByVariantId(@Param("variantId") UUID variantId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM offer WHERE variant_id = :variantId", nativeQuery = true)
    void deleteOffersByVariantId(@Param("variantId") UUID variantId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM recently_viewed WHERE variant_id = :variantId", nativeQuery = true)
    void deleteRecentlyViewedByVariantId(@Param("variantId") UUID variantId);
}
