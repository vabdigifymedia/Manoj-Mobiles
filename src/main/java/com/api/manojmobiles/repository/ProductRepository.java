package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByCategoryId(UUID categoryId);
    List<Product> findByBrandId(UUID brandId);
    List<Product> findByStatus(ProductStatus status);
}
