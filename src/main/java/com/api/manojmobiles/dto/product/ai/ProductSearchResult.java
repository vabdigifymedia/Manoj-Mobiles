package com.api.manojmobiles.dto.product.ai;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductSearchResult(
        UUID id,
        String name,
        String brand,
        String category,
        BigDecimal startingPrice
) {}
