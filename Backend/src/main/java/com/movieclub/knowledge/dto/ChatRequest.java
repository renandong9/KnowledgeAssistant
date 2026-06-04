package com.movieclub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    private Long sessionId;
    @NotBlank
    private String question;
    private Integer limit = 6;
}
