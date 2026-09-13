package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.dto.request.AiChatRequest;
import com.api.manojmobiles.service.ai.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/ai")
@RequiredArgsConstructor
public class PublicAiController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@Valid @RequestBody AiChatRequest aiChatRequest) {
        String response = aiChatService.chat(aiChatRequest.getMessage());

        return ResponseEntity.ok(response);
    }
}
