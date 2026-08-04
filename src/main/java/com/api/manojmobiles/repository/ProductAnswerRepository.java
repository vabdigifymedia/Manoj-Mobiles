package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ProductAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAnswerRepository extends JpaRepository<ProductAnswer, UUID> {
    List<ProductAnswer> findByQuestionId(UUID questionId);
}
