package com.api.manojmobiles.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequestDTO {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Cart total is required")
    @DecimalMin(value = "0.0", message = "Cart total cannot be negative")
    private BigDecimal cartTotal;
}
