package com.api.manojmobiles.dto.order;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponseDTO {

    private String variantName;
    private String productName;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal subtotal;
}