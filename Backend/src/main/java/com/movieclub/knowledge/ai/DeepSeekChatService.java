package com.movieclub.knowledge.ai;

import com.movieclub.knowledge.config.KnowledgeProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekChatService {
    private final KnowledgeProperties properties;
    private final WebClient webClient;

    public DeepSeekChatService(KnowledgeProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getDeepseek().getBaseUrl())
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(properties.getDeepseek().getApiKey())) {
            return "DeepSeek API Key 未配置。当前已完成检索和上下文组装，请在 DEEPSEEK_API_KEY 中配置密钥后获得真实 AI 回答。\n\n" + userPrompt;
        }
        Map<String, Object> body = Map.of(
                "model", properties.getDeepseek().getChatModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );
        try {
            Map<?, ?> response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getDeepseek().getApiKey())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null) {
                return "DeepSeek 返回为空。";
            }
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return response.toString();
            }
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            return String.valueOf(message.get("content"));
        } catch (Exception e) {
            return "DeepSeek 调用失败：" + e.getMessage();
        }
    }
}
