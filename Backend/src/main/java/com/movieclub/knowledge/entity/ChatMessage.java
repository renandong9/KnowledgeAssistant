package com.movieclub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_messages")
public class ChatMessage {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String referencesJson;
    private LocalDateTime createTime;
}
