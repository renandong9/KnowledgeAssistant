package com.movieclub.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieclub.knowledge.ai.DeepSeekChatService;
import com.movieclub.knowledge.common.BusinessException;
import com.movieclub.knowledge.dto.ReviewRequest;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.entity.DocumentChunk;
import com.movieclub.knowledge.entity.ReviewReport;
import com.movieclub.knowledge.mapper.DocumentChunkMapper;
import com.movieclub.knowledge.mapper.DocumentMapper;
import com.movieclub.knowledge.mapper.ReviewReportMapper;
import com.movieclub.knowledge.service.ReviewService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final ReviewReportMapper reportMapper;
    private final DeepSeekChatService chatService;

    public ReviewServiceImpl(DocumentMapper documentMapper,
                             DocumentChunkMapper chunkMapper,
                             ReviewReportMapper reportMapper,
                             DeepSeekChatService chatService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.reportMapper = reportMapper;
        this.chatService = chatService;
    }

    @Override
    public ReviewReport summarize(ReviewRequest request) {
        if (request.getDocumentId() == null) {
            throw new BusinessException("documentId 不能为空");
        }
        Document document = documentMapper.selectById(request.getDocumentId());
        if (document == null) {
            throw new BusinessException("文档不存在：" + request.getDocumentId());
        }
        String content = chunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, request.getDocumentId())
                        .orderByAsc(DocumentChunk::getChunkIndex)
                        .last("LIMIT 12"))
                .stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.joining("\n\n"));
        String output = chatService.chat(
                "你是学习复盘助手，请基于文档内容输出：总结、核心知识点、复习问题、薄弱点和提升建议。",
                content
        );
        ReviewReport report = new ReviewReport();
        report.setDocumentId(document.getId());
        report.setTitle(request.getTitle() == null || request.getTitle().isBlank() ? document.getTitle() + " 学习复盘" : request.getTitle());
        report.setSummary(output);
        report.setKeyPoints(output);
        report.setQuestions(output);
        report.setImprovementAdvice(output);
        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);
        return report;
    }
}
