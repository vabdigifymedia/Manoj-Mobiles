package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.coupon.*;
import com.api.manojmobiles.entity.Coupon;
import com.api.manojmobiles.entity.CouponUsage;
import com.api.manojmobiles.entity.Order;
import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.DiscountType;
import com.api.manojmobiles.exception.BadRequestException;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.CouponRepository;
import com.api.manojmobiles.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Transactional
    public CouponResponseDTO createCoupon(CreateCouponRequestDTO request) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Coupon code '" + request.getCode() + "' already exists");
        }

        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new BadRequestException("validTo date cannot be before validFrom date");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType())
                .value(request.getValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .usageLimitPerUser(request.getUsageLimitPerUser())
                .totalUsageCap(request.getTotalUsageCap())
                .isActive(true)
                .build();

        coupon = couponRepository.save(coupon);
        return mapToDTO(coupon);
    }

    @Transactional
    public CouponResponseDTO updateCoupon(UUID id, UpdateCouponRequestDTO request) {
        Coupon coupon = getCouponEntityById(id);

        if (request.getDiscountType() != null) coupon.setDiscountType(request.getDiscountType());
        if (request.getValue() != null) coupon.setValue(request.getValue());
        if (request.getMinOrderAmount() != null) coupon.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getValidFrom() != null) coupon.setValidFrom(request.getValidFrom());
        if (request.getValidTo() != null) coupon.setValidTo(request.getValidTo());
        if (request.getUsageLimitPerUser() != null) coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());
        if (request.getTotalUsageCap() != null) coupon.setTotalUsageCap(request.getTotalUsageCap());
        if (request.getIsActive() != null) coupon.setIsActive(request.getIsActive());

        if (coupon.getValidTo().isBefore(coupon.getValidFrom())) {
            throw new BadRequestException("validTo date cannot be before validFrom date");
        }

        coupon = couponRepository.save(coupon);
        return mapToDTO(coupon);
    }

    @Transactional(readOnly = true)
    public List<CouponResponseDTO> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CouponResponseDTO getCouponById(UUID id) {
        return mapToDTO(getCouponEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CouponUsageDTO> getCouponUsageStats(UUID id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found with id: " + id);
        }
        return couponUsageRepository.findByCouponIdOrderByUsedAtDesc(id).stream()
                .map(usage -> CouponUsageDTO.builder()
                        .id(usage.getId())
                        .couponCode(usage.getCoupon().getCode())
                        .userEmail(usage.getUser().getEmail())
                        .userName(usage.getUser().getName())
                        .orderNumber(usage.getOrder().getOrderNumber())
                        .usedAt(usage.getUsedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCoupon(UUID id) {
        Coupon coupon = getCouponEntityById(id);
        long usageCount = couponUsageRepository.countByCouponId(id);

        if (usageCount > 0) {
            log.info("Coupon {} has usage history. Soft deleting.", coupon.getCode());
            coupon.setIsActive(false);
            couponRepository.save(coupon);
        } else {
            log.info("Coupon {} has no usage history. Hard deleting.", coupon.getCode());
            couponRepository.delete(coupon);
        }
    }

    @Transactional(readOnly = true)
    public ApplyCouponResponseDTO previewCouponDiscount(String code, BigDecimal cartTotal, User user) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        BigDecimal discountAmount = validateAndCalculateDiscount(coupon, cartTotal, user);
        BigDecimal finalAmount = cartTotal.subtract(discountAmount).max(BigDecimal.ZERO);

        return ApplyCouponResponseDTO.builder()
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getValue())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .message("Coupon applied successfully. You saved ₹" + discountAmount)
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal validateAndCalculateDiscount(Coupon coupon, BigDecimal cartTotal, User user) {
        LocalDate today = LocalDate.now();

        if (Boolean.FALSE.equals(coupon.getIsActive())) {
            throw new BadRequestException("Coupon is not active");
        }

        if (today.isBefore(coupon.getValidFrom())) {
            throw new BadRequestException("Coupon is not active yet");
        }

        if (today.isAfter(coupon.getValidTo())) {
            throw new BadRequestException("Coupon has expired");
        }

        if (cartTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Minimum order amount of ₹" + coupon.getMinOrderAmount() + " required for this coupon");
        }

        if (coupon.getTotalUsageCap() != null) {
            long totalUsage = couponUsageRepository.countByCouponId(coupon.getId());
            if (totalUsage >= coupon.getTotalUsageCap()) {
                throw new BadRequestException("Coupon overall usage limit reached");
            }
        }

        long userUsageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), user.getId());
        if (coupon.getUsageLimitPerUser() != null && userUsageCount >= coupon.getUsageLimitPerUser()) {
            throw new BadRequestException("You have already used this coupon maximum allowed times");
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == DiscountType.FLAT) {
            discountAmount = coupon.getValue();
        } else if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discountAmount = cartTotal.multiply(coupon.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        // Discount cannot exceed cart total
        return discountAmount.min(cartTotal);
    }

    @Transactional
    public void recordCouponUsage(Coupon coupon, User user, Order order) {
        try {
            CouponUsage usage = CouponUsage.builder()
                    .coupon(coupon)
                    .user(user)
                    .order(order)
                    .usedAt(LocalDateTime.now())
                    .build();
            couponUsageRepository.saveAndFlush(usage);
        } catch (DataIntegrityViolationException e) {
            log.error("Concurrent coupon usage detected for user {} and coupon {}", user.getEmail(), coupon.getCode(), e);
            throw new BadRequestException("You have already used this coupon");
        }
    }

    @Transactional(readOnly = true)
    public List<ActiveCouponResponseDTO> getActiveCouponsForUser(User user) {
        LocalDate today = LocalDate.now();
        List<Coupon> activeCoupons = couponRepository.findByIsActiveTrueAndValidFromLessThanEqualAndValidToGreaterThanEqual(today, today);

        return activeCoupons.stream().map(coupon -> {
            long userUsageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), user.getId());
            boolean alreadyUsedByYou = userUsageCount >= coupon.getUsageLimitPerUser();

            return ActiveCouponResponseDTO.builder()
                    .id(coupon.getId())
                    .code(coupon.getCode())
                    .discountType(coupon.getDiscountType())
                    .value(coupon.getValue())
                    .minOrderAmount(coupon.getMinOrderAmount())
                    .validFrom(coupon.getValidFrom())
                    .validTo(coupon.getValidTo())
                    .alreadyUsedByYou(alreadyUsedByYou)
                    .build();
        }).collect(Collectors.toList());
    }

    public Coupon getCouponEntityByCode(String code) {
        return couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
    }

    private Coupon getCouponEntityById(UUID id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
    }

    private CouponResponseDTO mapToDTO(Coupon coupon) {
        long totalUsedCount = couponUsageRepository.countByCouponId(coupon.getId());
        boolean isExpired = LocalDate.now().isAfter(coupon.getValidTo());

        return CouponResponseDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .value(coupon.getValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .usageLimitPerUser(coupon.getUsageLimitPerUser())
                .totalUsageCap(coupon.getTotalUsageCap())
                .isActive(coupon.getIsActive())
                .totalUsedCount(totalUsedCount)
                .isExpired(isExpired)
                .build();
    }
}
