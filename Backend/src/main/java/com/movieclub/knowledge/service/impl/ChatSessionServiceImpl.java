package com.movieclub.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieclub.knowledge.common.BusinessException;
import com.movieclub.knowledge.dto.ChatSessionCreateRequest;
import com.movieclub.knowledge.dto.ChatSessionUpdateRequest;
import com.movieclub.knowledge.entity.ChatMessage;
import com.movieclub.knowledge.entity.ChatSession;
import com.movieclub.knowledge.mapper.ChatMessageMapper;
import com.movieclub.knowledge.mapper.ChatSessionMapper;
import com.movieclub.knowledge.mapper.DocumentMapper;
import com.movieclub.knowledge.service.ChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {
    private static final String DEFAULT_TITLE = "默认问答";

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final DocumentMapper documentMapper;

    public ChatSessionServiceImpl(ChatSessionMapper sessionMapper,
                                  ChatMessageMapper messageMapper,
                                  DocumentMapper documentMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.documentMapper = documentMapper;
    }

    @Override
    @Transactional
    public List<ChatSession> listByDocument(Long documentId) {
        ensureDocumentExists(documentId);
        List<ChatSession> sessions = selectByDocument(documentId);
        if (!sessions.isEmpty()) {
            return sessions;
        }
        ChatSessionCreateRequest request = new ChatSessionCreateRequest();
        request.setDocumentId(documentId);
        request.setTitle(DEFAULT_TITLE);
        request.setType("default");
        create(request);
        return selectByDocument(documentId);
    }

    @Override
    public ChatSession create(ChatSessionCreateRequest request) {
        ensureDocumentExists(request.getDocumentId());
        LocalDateTime now = LocalDateTime.now();
        ChatSession session = new ChatSession();
        session.setDocumentId(request.getDocumentId());
        session.setTitle(request.getTitle().trim());
        session.setType(request.getType() == null || request.getType().isBlank() ? "custom" : request.getType().trim());
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public ChatSession update(Long sessionId, ChatSessionUpdateRequest request) {
        ChatSession session = getSession(sessionId);
        session.setTitle(request.getTitle().trim());
        if (request.getType() != null && !request.getType().isBlank()) {
            session.setType(request.getType().trim());
        }
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return session;
    }

    @Override
    @Transactional
    public void delete(Long sessionId) {
        getSession(sessionId);
        sessionMapper.deleteById(sessionId);
    }

    @Override
    public List<ChatMessage> listMessages(Long sessionId) {
        getSession(sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime));
    }

    private List<ChatSession> selectByDocument(Long documentId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getDocumentId, documentId)
                .orderByDesc(ChatSession::getUpdateTime));
    }

    private ChatSession getSession(Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("提问窗口不存在：" + sessionId);
        }
        return session;
    }

    private void ensureDocumentExists(Long documentId) {
        if (documentId == null || documentMapper.selectById(documentId) == null) {
            throw new BusinessException("文档不存在：" + documentId);
        }
    }
}
