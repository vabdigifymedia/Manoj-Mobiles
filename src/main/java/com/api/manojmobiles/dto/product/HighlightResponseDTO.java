package com.api.manojmobiles.dto.product;

import com.api.manojmobiles.entity.enums.AllowedIcon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HighlightResponseDTO {
    private UUID id;
    private AllowedIcon iconName;
    private String text;
    private Integer displayOrder;
}
