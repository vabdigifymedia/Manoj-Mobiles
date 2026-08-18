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
public class ProductListResponseDTO {

    private UUID id;
    private String name;
    private String brandName;
    private String categoryName;
    private UUID categoryId;
    private String slug;
    private String status;

    // Summary fields for catalog grid display
    private UUID defaultVariantId;
    private BigDecimal startingPrice;
    private String primaryImageUrl;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private BigDecimal mrp;
    private Integer discountPercent;
    private List<String> highlights;
}
