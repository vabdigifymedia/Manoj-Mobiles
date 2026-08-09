package com.api.manojmobiles.dto.coupon;

import com.api.manojmobiles.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveCouponResponseDTO {
    private UUID id;
    private String code;
    private DiscountType discountType;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private LocalDate validFrom;
    private LocalDate validTo;
    
    // Key indicator for customer UI
    private boolean alreadyUsedByYou;
}
