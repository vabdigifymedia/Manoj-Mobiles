package com.api.manojmobiles.dto.brand;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandResponseDTO {

    private UUID id;
    private String name;
    private String logoUrl;
    private String description;
    private String slug;
}