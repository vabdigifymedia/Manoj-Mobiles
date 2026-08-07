package com.api.manojmobiles.dto.cart;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponseDTO {

    private UUID id;
    private UUID variantId;
    private String variantName;
    private String productName;
    private String sku;
    private String primaryImage;
    private Integer qty;
    private BigDecimal priceAtAdd;
    private BigDecimal currentPrice;
    private BigDecimal subtotal;
    private String stockStatus;
    private Boolean isAvailable;
}