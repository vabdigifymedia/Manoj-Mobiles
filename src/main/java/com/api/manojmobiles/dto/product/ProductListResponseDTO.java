package com.api.manojmobiles.dto.product;

import lombok.*;

import java.math.BigDecimal;
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
    private String slug;
    private String status;

    // Summary fields for catalog grid display
    private BigDecimal startingPrice;
    private String primaryImageUrl;
    private BigDecimal avgRating;
    private Integer totalReviews;
}
