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
        return answer(question, null, references);
    }

    public String answer(String question, String topic, List<SearchResult> references) {
        if (references == null || references.isEmpty()) {
            return "原文没有足够信息。";
        }
        String context = references.stream()
                .map(result -> "[Document: " + result.getTitle() + ", chunk: " + result.getChunkId() + "]\n" + result.getContent())
                .collect(Collectors.joining("\n\n"));
        String prompt = "当前提问窗口：" + (topic == null || topic.isBlank() ? "默认问答" : topic)
                + "\n问题：\n" + question
                + "\n\n原文 Chunk：\n" + context;
        return aiChatService.chat(
                "你是科研论文知识助手。只能基于给定原文 Chunk 回答，必须引用相关 chunk id。"
                        + " 如果原文中找不到依据，必须明确回答“原文没有足够信息”，不要编造。",
                prompt
        );
    }
}
