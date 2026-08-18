package com.api.manojmobiles.dto.personalization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompareItemDTO {
    private UUID id;
    private UUID variantId;
    private UUID productId;
    private String variantName;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
    private Map<String, String> specifications;
}
