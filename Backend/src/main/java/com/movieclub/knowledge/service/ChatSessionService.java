package com.movieclub.knowledge.service;

import com.movieclub.knowledge.dto.ChatSessionCreateRequest;
import com.movieclub.knowledge.dto.ChatSessionUpdateRequest;
import com.movieclub.knowledge.entity.ChatMessage;
import com.movieclub.knowledge.entity.ChatSession;

import java.util.List;

public interface ChatSessionService {
    List<ChatSession> listByDocument(Long documentId);

    ChatSession create(ChatSessionCreateRequest request);

    ChatSession update(Long sessionId, ChatSessionUpdateRequest request);

    void delete(Long sessionId);

    List<ChatMessage> listMessages(Long sessionId);
}
