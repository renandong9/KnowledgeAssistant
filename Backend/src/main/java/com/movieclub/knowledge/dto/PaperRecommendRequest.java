package com.movieclub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaperRecommendRequest {
    @NotBlank
    private String query;
    private Integer limit = 5;
}
