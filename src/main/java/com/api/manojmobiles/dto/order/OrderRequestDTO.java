package com.api.manojmobiles.dto.order;

import com.api.manojmobiles.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    @NotNull
    private UUID addressId;

    private String couponCode;

    @NotNull
    private PaymentMethod paymentMethod;
}