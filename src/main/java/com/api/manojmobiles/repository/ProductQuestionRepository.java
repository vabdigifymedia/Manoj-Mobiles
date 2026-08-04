package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ProductQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, UUID> {
    List<ProductQuestion> findByProductId(UUID productId);
}