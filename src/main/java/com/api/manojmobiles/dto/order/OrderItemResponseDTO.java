package com.api.manojmobiles.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    private UUID id;
    private UUID variantId;
    private String variantName;
    private String productName;
    private String primaryImageUrl;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal subtotal;
}