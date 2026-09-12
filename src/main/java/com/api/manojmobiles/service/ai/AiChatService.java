package com.api.manojmobiles.service.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final ChatClient chatClient;

    public AiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are Manoj AI, a helpful and friendly mobile store assistant for Manoj Mobiles. Your job is to help customers find mobile phones, compare specifications, and answer questions. Use the provided tools to search the database.")
                .defaultFunctions("searchMobiles")
                .build();
    }

    public String chat(String message, List<Map<String, Object>> history) {
        List<Message> messages = new ArrayList<>();

        if (history != null) {
            for (Map<String, Object> turn : history) {
                String role = (String) turn.get("role");
                String content = (String) turn.get("content");
                
                if (content != null) {
                    if ("user".equalsIgnoreCase(role)) {
                        messages.add(new UserMessage(content));
                    } else if ("assistant".equalsIgnoreCase(role) || "model".equalsIgnoreCase(role)) {
                        messages.add(new AssistantMessage(content));
                    }
                }
            }
        }
        
        messages.add(new UserMessage(message));

        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }
}
