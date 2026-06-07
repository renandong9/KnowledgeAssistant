package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.ai.AiChatService;
import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.dto.AiChatRequest;
import com.movieclub.knowledge.vo.AiChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiChatService aiChatService;

    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        String answer = aiChatService.chat(request.getSystemPrompt(), request.getMessage());
        return ApiResponse.ok(new AiChatResponse(answer));
    }
}
