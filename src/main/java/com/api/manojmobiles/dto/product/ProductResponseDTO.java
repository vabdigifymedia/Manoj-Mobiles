package com.api.manojmobiles.dto.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private UUID id;
    private String name;
    private UUID brandId;
    private String brandName;
    private UUID categoryId;
    private String categoryName;
    private String description;
    private String status;
    private Integer warrantyMonths;
    private Integer returnPolicyDays;
    private Boolean isReturnable;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private String slug;
    private List<ProductVariantResponseDTO> variants;
    private List<HighlightResponseDTO> highlights;
}