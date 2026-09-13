package com.api.manojmobiles.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiChatRequest {

    @NotBlank(message = "Message cannot be Blank")
    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}
