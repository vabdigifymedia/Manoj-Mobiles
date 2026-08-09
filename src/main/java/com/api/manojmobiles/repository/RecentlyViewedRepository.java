package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.RecentlyViewed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, UUID> {
    List<RecentlyViewed> findByUserIdOrderByViewedAtDesc(UUID userId);
    Optional<RecentlyViewed> findByUserIdAndVariantId(UUID userId, UUID variantId);
    void deleteByUserId(UUID userId);
}