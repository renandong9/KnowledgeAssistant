package com.movieclub.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {
    private String storageDir;
    private DeepSeek deepseek = new DeepSeek();
    private Paper paper = new Paper();

    @Data
    public static class DeepSeek {
        private String apiKey;
        private String baseUrl;
        private String chatModel;
        private String embeddingModel;
    }

    @Data
    public static class Paper {
        private String semanticScholarUrl;
        private String arxivUrl;
    }
}
