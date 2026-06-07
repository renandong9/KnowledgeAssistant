package com.movieclub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {
    private String systemPrompt = "You are a helpful assistant.";
    @NotBlank
    private String message;
}
