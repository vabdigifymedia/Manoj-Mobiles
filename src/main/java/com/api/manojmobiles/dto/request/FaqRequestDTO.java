package com.api.manojmobiles.dto.request;

import com.api.manojmobiles.entity.enums.FaqCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaqRequestDTO {
    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    private FaqCategory category = FaqCategory.GENERAL;
    private Integer displayOrder = 0;
    private Boolean isActive = true;
}
