package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Coupon> findByIsActiveTrueAndValidFromLessThanEqualAndValidToGreaterThanEqual(LocalDate date1, LocalDate date2);
}