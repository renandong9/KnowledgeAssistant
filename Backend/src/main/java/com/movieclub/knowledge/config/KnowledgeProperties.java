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
    private Ocr ocr = new Ocr();
    private Paper paper = new Paper();

    @Data
    public static class Ollama {
        private String baseUrl;
        private String model;
        private String embeddingModel;
    }

    @Data
    public static class Ocr {
        private boolean enabled;
        private String provider;
        private String tesseractDataPath;
        private String language;
        private int dpi;
        private int maxPages;
    }

    @Data
    public static class Paper {
        private String semanticScholarUrl;
        private String semanticScholarApiKey;
        private String arxivUrl;
    }
}
