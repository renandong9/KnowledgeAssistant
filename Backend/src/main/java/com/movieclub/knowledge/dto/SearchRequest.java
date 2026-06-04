package com.movieclub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank
    private String query;
    private String mode = "hybrid";
    private Integer limit = 8;
}
