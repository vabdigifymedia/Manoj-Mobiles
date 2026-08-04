package com.api.manojmobiles.dto.category;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {

    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
    private List<CategoryResponseDTO> children;
}