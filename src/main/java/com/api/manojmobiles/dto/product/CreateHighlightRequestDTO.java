package com.api.manojmobiles.dto.product;

import com.api.manojmobiles.entity.enums.AllowedIcon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateHighlightRequestDTO {

    @NotNull(message = "Icon name is required")
    private AllowedIcon iconName;

    @NotBlank(message = "Highlight text is required")
    private String text;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;
}
