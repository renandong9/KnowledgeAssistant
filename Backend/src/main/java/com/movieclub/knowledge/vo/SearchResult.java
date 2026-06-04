package com.movieclub.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private Long documentId;
    private Long chunkId;
    private String title;
    private String content;
    private Integer pageNumber;
    private Double score;
}
