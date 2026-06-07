package com.movieclub.knowledge.ai;

import com.movieclub.knowledge.entity.ChatMessage;
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

    public String answer(String question, String topic, List<SearchResult> references) {
        return answer(question, topic, references, List.of());
    }

    public String answer(String question, String topic, List<SearchResult> references, List<ChatMessage> history) {
        if (references == null || references.isEmpty()) {
            return "原文没有足够信息。";
        }
        String context = references.stream()
                .map(result -> "[Document: " + result.getTitle()
                        + ", chunk: " + result.getChunkId()
                        + ", page: " + (result.getPageNumber() == null ? "unknown" : result.getPageNumber())
                        + "]\n" + result.getContent())
                .collect(Collectors.joining("\n\n"));
        String historyText = history == null ? "" : history.stream()
                .map(message -> message.getRole() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));
        String prompt = "当前提问窗口：" + (topic == null || topic.isBlank() ? "默认问答" : topic)
                + "\n当前窗口历史对话（只用于理解追问，不可作为事实依据）：\n" + historyText
                + "\n\n用户问题：\n" + question
                + "\n\n原文 Chunk：\n" + context;
        return aiChatService.chat(
                "你是科研论文知识助手。回答必须基于给定原文 Chunk，不要编造。"
                        + " 回答中尽量标注依据来自哪些 chunk id 或页码。"
                        + " 如果原文中找不到足够依据，必须明确回答“原文没有足够信息”。",
                prompt
        );
    }
}
