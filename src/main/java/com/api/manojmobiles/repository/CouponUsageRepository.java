package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    List<CouponUsage> findByCouponId(UUID couponId);
    long countByCouponIdAndUserId(UUID couponId, UUID userId);
}