package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.CompareList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompareListRepository extends JpaRepository<CompareList, UUID> {
    List<CompareList> findByUserId(UUID userId);
    Optional<CompareList> findByUserIdAndVariantId(UUID userId, UUID variantId);
    void deleteByUserIdAndVariantId(UUID userId, UUID variantId);
    void deleteByUserId(UUID userId);
    long countByUserId(UUID userId);
}