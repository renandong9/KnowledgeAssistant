package com.movieclub.knowledge.ai;

import com.movieclub.knowledge.vo.SearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final AiChatService aiChatService;

    public RagService(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    public String answer(String question, List<SearchResult> references) {
        String context = references.stream()
                .map(result -> "[Document: " + result.getTitle() + ", chunk: " + result.getChunkId() + "]\n" + result.getContent())
                .collect(Collectors.joining("\n\n"));
        String prompt = "Question:\n" + question + "\n\nKnowledge base context:\n" + context;
        return aiChatService.chat(
                "You are a personal knowledge base assistant. Answer only from the given context. If the context is insufficient, say so clearly and suggest the next learning step.",
                prompt
        );
    }
}
