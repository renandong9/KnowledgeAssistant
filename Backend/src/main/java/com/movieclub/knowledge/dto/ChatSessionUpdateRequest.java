package com.movieclub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSessionUpdateRequest {
    @NotBlank
    private String title;
    private String type;
}
