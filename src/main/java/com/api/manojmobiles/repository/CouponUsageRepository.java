package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    List<CouponUsage> findByCouponIdOrderByUsedAtDesc(UUID couponId);
    long countByCouponId(UUID couponId);
    long countByCouponIdAndUserId(UUID couponId, UUID userId);
}