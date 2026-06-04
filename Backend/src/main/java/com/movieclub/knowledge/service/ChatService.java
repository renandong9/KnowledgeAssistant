package com.movieclub.knowledge.service;

import com.movieclub.knowledge.dto.ChatRequest;
import com.movieclub.knowledge.vo.ChatResponse;

public interface ChatService {
    ChatResponse chat(ChatRequest request);
}
