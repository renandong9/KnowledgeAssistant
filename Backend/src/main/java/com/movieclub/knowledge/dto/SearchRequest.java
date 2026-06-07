package com.movieclub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    private Long documentId;
    @NotBlank
    private String query;
    private String topic;
    private String mode = "hybrid";
    private Integer limit = 8;
}
