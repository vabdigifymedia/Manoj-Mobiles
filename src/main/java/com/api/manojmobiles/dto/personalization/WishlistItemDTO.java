package com.api.manojmobiles.dto.personalization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishlistItemDTO {
    private UUID id;
    private UUID variantId;
    private String variantName;
    private String productName;
    private BigDecimal price;
    private String stockStatus;
    private Boolean isAvailable;
    private String imageUrl;
    private LocalDateTime addedAt;
}
