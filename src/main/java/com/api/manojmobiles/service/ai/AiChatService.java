package com.api.manojmobiles.service.ai;

import com.api.manojmobiles.service.ai.tools.ProductTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final ChatClient chatClient;

    public AiChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ProductTools productTools) {

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are Manoj AI, the official AI assistant for Manoj Mobiles.
                        
                                            Your primary role is to help customers with:
                                            - Mobile phones
                                            - Smartphones
                                            - Mobile accessories
                                            - Product-related questions
                                            - Basic technical specifications
                                            - Comparisons between products
                                            - General assistance related to Manoj Mobiles
                        
                                            Do not answer unrelated questions such as:
                                            - Politics
                                            - Coding
                                            - General trivia
                                            - Medical advice
                                            - Legal advice
                                            - Personal advice
                        
                                            If a question is unrelated to Manoj Mobiles or mobile products,
                                            politely say:
                                            "I can help you with mobile phones, accessories, and Manoj Mobiles-related questions."
                        
                                            Never pretend to have information that you don't actually have.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(productTools)
                .build();
    }

    public String chat(String chatId, String message){
        //logic to call AI
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }
}
