package com.api.manojmobiles.dto.review;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class RatingSummaryDTO {
    private UUID productId;
    private BigDecimal avgRating;
    private Integer totalReviews;

    private Long fiveStarCount;
    private Long fourStarCount;
    private Long threeStarCount;
    private Long twoStarCount;
    private Long oneStarCount;

    private Double fiveStarPercentage;
    private Double fourStarPercentage;
    private Double threeStarPercentage;
    private Double twoStarPercentage;
    private Double oneStarPercentage;
}
