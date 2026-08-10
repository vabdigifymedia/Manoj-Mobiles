package com.api.manojmobiles.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequestDTO {

    @NotNull
    private UUID productId;

    @NotBlank
    private String variantName;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String color;

    @NotNull(message = "MRP is required")
    @DecimalMin("0.0")
    private BigDecimal mrp;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal sellingPrice;

    @DecimalMin("0.0")
    private BigDecimal gstPercent;

    @NotNull
    @Min(0)
    private Integer stockQty;

    private Boolean codAvailable;
}