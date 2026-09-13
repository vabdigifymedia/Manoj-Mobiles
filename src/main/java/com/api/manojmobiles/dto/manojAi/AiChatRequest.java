package com.api.manojmobiles.dto.manojAi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {

    private String chatId;

    @NotBlank(message = "Message cannot be Blank")
    @Size(max = 100, message = "Message cannot exceed 100 characters")
    private String message;
}
