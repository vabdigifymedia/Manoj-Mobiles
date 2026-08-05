package com.api.manojmobiles.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecificationRequestDTO {

    private UUID variantId;

    @NotBlank
    private String specGroup;

    @NotBlank
    private String specKey;

    @NotBlank
    private String specValue;
}