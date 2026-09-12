package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.dto.request.AiChatRequest;
import com.api.manojmobiles.service.ai.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/ai")
@CrossOrigin(origins = "*")
public class PublicAiController {

    private final AiChatService aiChatService;

    public PublicAiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody AiChatRequest request) {
        String response = aiChatService.chat(request.getMessage(), request.getHistory());
        return ResponseEntity.ok(Map.of("message", response));
    }
}
