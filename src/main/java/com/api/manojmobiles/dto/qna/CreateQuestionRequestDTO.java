package com.api.manojmobiles.dto.qna;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateQuestionRequestDTO {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "Question text is required")
    private String questionText;
}
