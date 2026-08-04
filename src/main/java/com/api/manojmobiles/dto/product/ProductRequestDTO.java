package com.api.manojmobiles.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    @NotBlank
    private String name;

    @NotNull
    private UUID brandId;

    @NotNull
    private UUID categoryId;

    private String description;

    @Min(0)
    private Integer warrantyMonths;

    @Min(0)
    private Integer returnPolicyDays;

    private Boolean isReturnable;
}