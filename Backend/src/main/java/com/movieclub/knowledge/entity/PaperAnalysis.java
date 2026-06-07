package com.movieclub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paper_analysis")
public class PaperAnalysis {
    private Long id;
    private Long documentId;
    private String researchBackground;
    private String problemDefinition;
    private String coreMethod;
    private String experimentResults;
    private String innovationPoints;
    private String strengths;
    private String weaknesses;
    private String oneSentenceSummary;
    private String modelName;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
