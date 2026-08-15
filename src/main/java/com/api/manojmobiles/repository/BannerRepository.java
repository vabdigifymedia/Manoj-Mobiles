package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Banner;
import com.api.manojmobiles.entity.enums.BannerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    @Query("SELECT b FROM Banner b WHERE b.isActive = true " +
           "AND (:type IS NULL OR b.bannerType = :type) " +
           "AND (b.startTime IS NULL OR b.startTime <= :now) " +
           "AND (b.endTime IS NULL OR b.endTime >= :now) " +
           "ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findActiveBanners(@Param("type") BannerType type, @Param("now") Instant now);

    List<Banner> findAllByOrderByBannerTypeAscDisplayOrderAscCreatedAtDesc();
}
