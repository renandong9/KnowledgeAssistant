package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.dto.ChatSessionCreateRequest;
import com.movieclub.knowledge.dto.ChatSessionUpdateRequest;
import com.movieclub.knowledge.entity.ChatMessage;
import com.movieclub.knowledge.entity.ChatSession;
import com.movieclub.knowledge.service.ChatSessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatSessionController {
    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @GetMapping("/documents/{documentId}/chat-sessions")
    public ApiResponse<List<ChatSession>> listByDocument(@PathVariable Long documentId) {
        return ApiResponse.ok(chatSessionService.listByDocument(documentId));
    }

    @PostMapping("/documents/{documentId}/chat-sessions")
    public ApiResponse<ChatSession> create(@PathVariable Long documentId,
                                           @Valid @RequestBody ChatSessionCreateRequest request) {
        request.setDocumentId(documentId);
        return ApiResponse.ok(chatSessionService.create(request));
    }

    @PutMapping("/chat-sessions/{sessionId}")
    public ApiResponse<ChatSession> update(@PathVariable Long sessionId,
                                           @Valid @RequestBody ChatSessionUpdateRequest request) {
        return ApiResponse.ok(chatSessionService.update(sessionId, request));
    }

    @DeleteMapping("/chat-sessions/{sessionId}")
    public ApiResponse<Void> delete(@PathVariable Long sessionId) {
        chatSessionService.delete(sessionId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/chat-sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.ok(chatSessionService.listMessages(sessionId));
    }
}
