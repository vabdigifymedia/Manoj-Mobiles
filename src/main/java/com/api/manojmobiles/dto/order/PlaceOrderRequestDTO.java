package com.api.manojmobiles.dto.order;

import com.api.manojmobiles.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequestDTO {
    
    @NotNull(message = "Address ID is required")
    private UUID addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String couponCode;
}
