package com.api.manojmobiles.dto.qna;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuestionResponseDTO {
    private UUID id;
    private UUID productId;
    private String productName;
    private UUID userId;
    private String userName;
    private String questionText;
    private LocalDateTime createdAt;
    private List<AnswerResponseDTO> answers;
}
