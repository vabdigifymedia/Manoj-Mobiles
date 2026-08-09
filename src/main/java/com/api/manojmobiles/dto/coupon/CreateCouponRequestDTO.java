package com.api.manojmobiles.dto.coupon;

import com.api.manojmobiles.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateCouponRequestDTO {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", message = "Discount value cannot be negative")
    private BigDecimal value;

    @DecimalMin(value = "0.0", message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to date is required")
    private LocalDate validTo;

    @Min(value = 1, message = "Usage limit per user must be at least 1")
    @Builder.Default
    private Integer usageLimitPerUser = 1;

    @Min(value = 1, message = "Total usage cap must be at least 1")
    private Integer totalUsageCap;
}
