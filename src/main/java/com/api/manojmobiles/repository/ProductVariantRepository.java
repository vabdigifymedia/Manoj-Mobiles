package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.enums.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductId(UUID productId);
    List<ProductVariant> findByStockStatus(StockStatus status);
}
