package com.api.manojmobiles.dto.product;

import com.api.manojmobiles.entity.enums.AllowedIcon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateHighlightRequestDTO {
    private AllowedIcon iconName;
    private String text;
}
