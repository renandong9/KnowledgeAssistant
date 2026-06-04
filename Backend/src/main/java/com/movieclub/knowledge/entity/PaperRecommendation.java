package com.movieclub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paper_recommendations")
public class PaperRecommendation {
    private Long id;
    private String source;
    private String queryText;
    private String title;
    private String authors;
    private Integer publishedYear;
    private String abstractText;
    private String url;
    private String reason;
    private LocalDateTime createdAt;
}
