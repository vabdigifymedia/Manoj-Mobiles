package com.api.manojmobiles.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductFilterRequestDTO {
    private String query;
    private UUID categoryId;
    private UUID brandId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private Boolean inStockOnly;
    private java.util.List<String> colors;
}
