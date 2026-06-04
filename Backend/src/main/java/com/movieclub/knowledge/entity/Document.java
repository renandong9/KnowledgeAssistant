package com.movieclub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("documents")
public class Document {
    private Long id;
    private String title;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String storagePath;
    private String parseStatus;
    private String summary;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
