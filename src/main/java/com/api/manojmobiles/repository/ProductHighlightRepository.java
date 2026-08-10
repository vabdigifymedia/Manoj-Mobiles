package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ProductHighlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ProductHighlightRepository extends JpaRepository<ProductHighlight, UUID> {
    List<ProductHighlight> findByProductIdOrderByDisplayOrderAsc(UUID productId);
    long countByProductId(UUID productId);
}
