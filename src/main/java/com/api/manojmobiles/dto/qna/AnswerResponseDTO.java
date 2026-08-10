package com.api.manojmobiles.dto.qna;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AnswerResponseDTO {
    private UUID id;
    private UUID userId;
    private String userName;
    private String answerText;
    private Boolean isSellerAnswer;
    private LocalDateTime createdAt;
}
