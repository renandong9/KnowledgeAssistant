package com.movieclub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSessionCreateRequest {
    private Long documentId;
    @NotBlank
    private String title;
    private String type = "custom";
}
