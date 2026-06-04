package com.movieclub.knowledge.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long documentId;
    private String title;
}
