package com.api.manojmobiles.dto.coupon;

import com.api.manojmobiles.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponRequestDTO {

    private DiscountType discountType;

    @DecimalMin(value = "0.0", message = "Discount value cannot be negative")
    private BigDecimal value;

    @DecimalMin(value = "0.0", message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;

    private LocalDate validFrom;
    private LocalDate validTo;

    @Min(value = 1, message = "Usage limit per user must be at least 1")
    private Integer usageLimitPerUser;

    @Min(value = 1, message = "Total usage cap must be at least 1")
    private Integer totalUsageCap;

    private Boolean isActive;
}
