package com.movieclub.knowledge.ai;

import com.movieclub.knowledge.vo.SearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final DeepSeekChatService deepSeekChatService;

    public RagService(DeepSeekChatService deepSeekChatService) {
        this.deepSeekChatService = deepSeekChatService;
    }

    public String answer(String question, List<SearchResult> references) {
        String context = references.stream()
                .map(result -> "[文档: " + result.getTitle() + ", chunk: " + result.getChunkId() + "]\n" + result.getContent())
                .collect(Collectors.joining("\n\n"));
        String prompt = "问题：\n" + question + "\n\n已检索到的知识库上下文：\n" + context;
        return deepSeekChatService.chat(
                "你是个人知识库助手。只能基于给定上下文回答；如果上下文不足，请明确说明，并给出下一步学习建议。",
                prompt
        );
    }
}
