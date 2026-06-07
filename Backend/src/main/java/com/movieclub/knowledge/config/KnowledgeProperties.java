package com.movieclub.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {
    private String storageDir;
    private Ollama ollama = new Ollama();
    private Paper paper = new Paper();

    @Data
    public static class Ollama {
        private String baseUrl;
        private String model;
    }

    @Data
    public static class Paper {
        private String semanticScholarUrl;
        private String arxivUrl;
    }
}
