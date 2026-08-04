package com.api.manojmobiles.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecificationResponseDTO {

    private String specGroup;
    private String specKey;
    private String specValue;
}