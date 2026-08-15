package com.api.manojmobiles.dto.response;

import com.api.manojmobiles.entity.enums.FaqCategory;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class FaqResponseDTO {
    private UUID id;
    private String question;
    private String answer;
    private FaqCategory category;
    private Integer displayOrder;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
