package com.api.manojmobiles.dto.manojAi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String chatId;
    private String message;
}
