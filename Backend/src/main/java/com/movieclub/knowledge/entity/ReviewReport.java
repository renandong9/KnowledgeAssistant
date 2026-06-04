package com.movieclub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_reports")
public class ReviewReport {
    private Long id;
    private Long documentId;
    private String title;
    private String summary;
    private String keyPoints;
    private String questions;
    private String improvementAdvice;
    private LocalDateTime createdAt;
}
