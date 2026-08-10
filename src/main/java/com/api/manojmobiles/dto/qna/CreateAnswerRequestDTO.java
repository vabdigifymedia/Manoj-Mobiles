package com.api.manojmobiles.dto.qna;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAnswerRequestDTO {
    @NotBlank(message = "Answer text is required")
    private String answerText;
}
