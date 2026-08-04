package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.RecentlyViewed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, UUID> {
    List<RecentlyViewed> findByUserIdOrderByViewedAtDesc(UUID userId);
}