package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ProductQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, UUID> {
    Page<ProductQuestion> findByProductIdAndIsApprovedTrue(UUID productId, Pageable pageable);
}