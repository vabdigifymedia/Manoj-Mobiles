package com.api.manojmobiles.controller.public_api;

import com.api.manojmobiles.config.RedisProperties;
import com.api.manojmobiles.dto.ApiResponse;
import com.api.manojmobiles.dto.request.AiChatRequest;
import com.api.manojmobiles.service.RateLimiterService;
import com.api.manojmobiles.service.ai.AiChatService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RateLimiterService rateLimiterService;
    private final RedisProperties redisProperties;

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(@Valid @RequestBody AiChatRequest aiChatRequest, HttpServletRequest request) {

        String ip = getClientIp(request);
        String rateLimitKey = "rate:chat:" + ip;

        rateLimiterService.checkRateLimit(
                rateLimitKey,
                redisProperties.getRate().getLoginLimit(),
                redisProperties.getRate().getLoginWindow()
        );
        String response = aiChatService.chat(aiChatRequest.getMessage());

        return ResponseEntity.ok(ApiResponse.success("Success", response));
    }
}
