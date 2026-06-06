package com.movieclub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("documents")
public class Document {
    private Long id;
    private String title;
    private String originalFileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String parseStatus;
    private String summary;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
