package com.movieclub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_chunks")
public class DocumentChunk {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer pageNumber;
    private String positionHint;
    private String embedding;
    private Integer tokenCount;
    private LocalDateTime createTime;
}
