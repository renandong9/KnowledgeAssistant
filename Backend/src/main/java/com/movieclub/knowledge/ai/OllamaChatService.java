package com.movieclub.knowledge.ai;

import com.movieclub.knowledge.config.KnowledgeProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaChatService implements AiChatService {
    private final KnowledgeProperties properties;
    private final WebClient.Builder webClientBuilder;

    public OllamaChatService(KnowledgeProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getOllama().getModel(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        try {
            Map<?, ?> response = webClientBuilder
                    .baseUrl(properties.getOllama().getBaseUrl())
                    .build()
                    .post()
                    .uri("/api/chat")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null) {
                return "Ollama returned an empty response.";
            }
            Object messageObject = response.get("message");
            if (messageObject instanceof Map<?, ?> message) {
                Object content = message.get("content");
                if (content != null) {
                    return String.valueOf(content);
                }
            }
            return response.toString();
        } catch (Exception e) {
            return "Ollama call failed: " + e.getMessage();
        }
    }
}
