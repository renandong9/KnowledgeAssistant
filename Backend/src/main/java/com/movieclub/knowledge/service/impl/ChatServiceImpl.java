package com.movieclub.knowledge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieclub.knowledge.ai.RagService;
import com.movieclub.knowledge.common.BusinessException;
import com.movieclub.knowledge.dto.ChatRequest;
import com.movieclub.knowledge.dto.SearchRequest;
import com.movieclub.knowledge.entity.ChatMessage;
import com.movieclub.knowledge.entity.ChatSession;
import com.movieclub.knowledge.mapper.ChatMessageMapper;
import com.movieclub.knowledge.mapper.ChatSessionMapper;
import com.movieclub.knowledge.mapper.DocumentMapper;
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
    private static final String DEFAULT_SESSION_TITLE = "默认问答";

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final DocumentMapper documentMapper;
    private final SearchService searchService;
    private final RagService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatServiceImpl(ChatSessionMapper sessionMapper,
                           ChatMessageMapper messageMapper,
                           DocumentMapper documentMapper,
                           SearchService searchService,
                           RagService ragService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.documentMapper = documentMapper;
        this.searchService = searchService;
        this.ragService = ragService;
    }

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        ChatSession session = ensureSession(request);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setDocumentId(session.getDocumentId());
        searchRequest.setQuery(request.getQuestion());
        searchRequest.setTopic(session.getTitle());
        searchRequest.setLimit(request.getLimit());
        List<SearchResult> references = searchService.search(searchRequest);
        saveMessage(session.getId(), "user", request.getQuestion(), null);
        String answer = ragService.answer(request.getQuestion(), session.getTitle(), references);
        saveMessage(session.getId(), "assistant", answer, toJson(references));
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return new ChatResponse(session.getId(), answer, references);
    }

    private ChatSession ensureSession(ChatRequest request) {
        if (request.getSessionId() != null) {
            ChatSession existing = sessionMapper.selectById(request.getSessionId());
            if (existing == null) {
                throw new BusinessException("提问窗口不存在：" + request.getSessionId());
            }
            if (request.getDocumentId() != null && existing.getDocumentId() != null
                    && !request.getDocumentId().equals(existing.getDocumentId())) {
                throw new BusinessException("提问窗口不属于当前文档");
            }
            return existing;
        }
        if (request.getDocumentId() == null || documentMapper.selectById(request.getDocumentId()) == null) {
            throw new BusinessException("请先选择文档");
        }
        LocalDateTime now = LocalDateTime.now();
        ChatSession session = new ChatSession();
        session.setDocumentId(request.getDocumentId());
        session.setTitle(DEFAULT_SESSION_TITLE);
        session.setType("default");
        session.setCreateTime(now);
        session.setUpdateTime(now);
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
