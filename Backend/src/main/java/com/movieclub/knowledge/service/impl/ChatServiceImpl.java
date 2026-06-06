package com.movieclub.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieclub.knowledge.ai.RagService;
import com.movieclub.knowledge.dto.ChatRequest;
import com.movieclub.knowledge.dto.SearchRequest;
import com.movieclub.knowledge.entity.ChatMessage;
import com.movieclub.knowledge.entity.ChatSession;
import com.movieclub.knowledge.mapper.ChatMessageMapper;
import com.movieclub.knowledge.mapper.ChatSessionMapper;
import com.movieclub.knowledge.service.ChatService;
import com.movieclub.knowledge.service.SearchService;
import com.movieclub.knowledge.vo.ChatResponse;
import com.movieclub.knowledge.vo.SearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final SearchService searchService;
    private final RagService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatServiceImpl(ChatSessionMapper sessionMapper,
                           ChatMessageMapper messageMapper,
                           SearchService searchService,
                           RagService ragService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.searchService = searchService;
        this.ragService = ragService;
    }

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        ChatSession session = ensureSession(request);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(request.getQuestion());
        searchRequest.setLimit(request.getLimit());
        List<SearchResult> references = searchService.search(searchRequest);
        saveMessage(session.getId(), "user", request.getQuestion(), null);
        String answer = ragService.answer(request.getQuestion(), references);
        saveMessage(session.getId(), "assistant", answer, toJson(references));
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return new ChatResponse(session.getId(), answer, references);
    }

    private ChatSession ensureSession(ChatRequest request) {
        if (request.getSessionId() != null) {
            ChatSession existing = sessionMapper.selectById(request.getSessionId());
            if (existing != null) {
                return existing;
            }
        }
        ChatSession session = new ChatSession();
        String title = request.getQuestion().length() > 30 ? request.getQuestion().substring(0, 30) : request.getQuestion();
        session.setTitle(title);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    private void saveMessage(Long sessionId, String role, String content, String referencesJson) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setReferencesJson(referencesJson);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
