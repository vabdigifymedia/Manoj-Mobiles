package com.api.manojmobiles.dto.cart;

import jakarta.validation.constraints.Min;
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
public class AddToCartRequestDTO {

    @NotNull
    private UUID variantId;

    @NotNull
    @Min(1)
    private Integer qty;
}
