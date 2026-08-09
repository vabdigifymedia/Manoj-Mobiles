package com.api.manojmobiles.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageDTO {
    private UUID id;
    private String couponCode;
    private String userEmail;
    private String userName;
    private String orderNumber;
    private LocalDateTime usedAt;
}
