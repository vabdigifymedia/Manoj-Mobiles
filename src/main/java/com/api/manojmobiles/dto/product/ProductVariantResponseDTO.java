package com.api.manojmobiles.dto.product;

import com.api.manojmobiles.entity.enums.StockStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponseDTO {

    private UUID id;
    private String variantName;
    private String sku;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private Integer discountPercent;
    private BigDecimal gstPercent;
    private Integer stockQty;
    private StockStatus stockStatus;
    private Boolean codAvailable;
    private List<String> imageUrls;
    private List<ProductSpecificationResponseDTO> specifications;
}