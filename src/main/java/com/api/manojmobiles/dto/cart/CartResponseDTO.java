package com.api.manojmobiles.dto.cart;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDTO {

    private UUID id;
    private List<CartItemResponseDTO> items;
    private BigDecimal cartTotal;
}