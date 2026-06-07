package com.movieclub.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieclub.knowledge.ai.AiChatService;
import com.movieclub.knowledge.common.BusinessException;
import com.movieclub.knowledge.config.KnowledgeProperties;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.entity.DocumentChunk;
import com.movieclub.knowledge.entity.PaperAnalysis;
import com.movieclub.knowledge.mapper.DocumentChunkMapper;
import com.movieclub.knowledge.mapper.DocumentMapper;
import com.movieclub.knowledge.mapper.PaperAnalysisMapper;
import com.movieclub.knowledge.service.PaperAnalysisService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PaperAnalysisServiceImpl implements PaperAnalysisService {
    private static final int ANALYSIS_CHUNK_LIMIT = 16;
    private static final String[] SECTION_NAMES = {
            "研究背景",
            "问题定义",
            "核心方法",
            "实验结果",
            "创新点",
            "优点",
            "缺点",
            "一句话总结"
    };

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final PaperAnalysisMapper analysisMapper;
    private final AiChatService aiChatService;
    private final KnowledgeProperties properties;

    public PaperAnalysisServiceImpl(DocumentMapper documentMapper,
                                    DocumentChunkMapper chunkMapper,
                                    PaperAnalysisMapper analysisMapper,
                                    AiChatService aiChatService,
                                    KnowledgeProperties properties) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.analysisMapper = analysisMapper;
        this.aiChatService = aiChatService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public PaperAnalysis analyze(Long documentId) {
        Document document = getDocument(documentId);
        LocalDateTime now = LocalDateTime.now();
        PaperAnalysis analysis = findExisting(documentId);
        if (analysis == null) {
            analysis = new PaperAnalysis();
            analysis.setDocumentId(documentId);
            analysis.setCreateTime(now);
        }
        analysis.setStatus("PROCESSING");
        analysis.setUpdateTime(now);
        analysis.setModelName(properties.getOllama().getModel());
        save(analysis);

        try {
            String content = loadContent(documentId);
            String output = aiChatService.chat(buildSystemPrompt(), buildUserPrompt(document, content));
            Map<String, String> sections = parseSections(output);
            analysis.setResearchBackground(value(sections, "研究背景", output));
            analysis.setProblemDefinition(value(sections, "问题定义", output));
            analysis.setCoreMethod(value(sections, "核心方法", output));
            analysis.setExperimentResults(value(sections, "实验结果", output));
            analysis.setInnovationPoints(value(sections, "创新点", output));
            analysis.setStrengths(value(sections, "优点", output));
            analysis.setWeaknesses(value(sections, "缺点", output));
            analysis.setOneSentenceSummary(value(sections, "一句话总结", firstLine(output)));
            analysis.setStatus("COMPLETED");
            analysis.setErrorMessage(null);
            analysis.setUpdateTime(LocalDateTime.now());
            save(analysis);

            document.setAnalysisStatus("COMPLETED");
            document.setSummary(analysis.getOneSentenceSummary());
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            return analysis;
        } catch (Exception e) {
            analysis.setStatus("FAILED");
            analysis.setErrorMessage(e.getMessage());
            analysis.setUpdateTime(LocalDateTime.now());
            save(analysis);

            document.setAnalysisStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            return analysis;
        }
    }

    @Override
    public PaperAnalysis getByDocument(Long documentId) {
        getDocument(documentId);
        return findExisting(documentId);
    }

    private Document getDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在：" + documentId);
        }
        return document;
    }

    private PaperAnalysis findExisting(Long documentId) {
        return analysisMapper.selectOne(new LambdaQueryWrapper<PaperAnalysis>()
                .eq(PaperAnalysis::getDocumentId, documentId)
                .last("LIMIT 1"));
    }

    private void save(PaperAnalysis analysis) {
        if (analysis.getId() == null) {
            analysisMapper.insert(analysis);
        } else {
            analysisMapper.updateById(analysis);
        }
    }

    private String loadContent(Long documentId) {
        List<DocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex)
                .last("LIMIT " + ANALYSIS_CHUNK_LIMIT));
        return chunks.stream()
                .map(chunk -> "chunk " + chunk.getChunkIndex() + ":\n" + chunk.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildSystemPrompt() {
        return "你是严谨的 AI 科研论文分析助手。必须基于原文内容分析，不确定就说明原文没有足够信息。";
    }

    private String buildUserPrompt(Document document, String content) {
        return "请基于以下论文原文，按固定标题输出：研究背景、问题定义、核心方法、实验结果、创新点、优点、缺点、一句话总结。\n"
                + "每个标题都必须出现；如果原文没有足够信息，请在对应标题下写“原文没有足够信息”。\n"
                + "文档标题：" + document.getTitle() + "\n\n原文：\n" + content;
    }

    private Map<String, String> parseSections(String output) {
        Map<String, String> sections = new LinkedHashMap<>();
        for (int i = 0; i < SECTION_NAMES.length; i++) {
            String current = Pattern.quote(SECTION_NAMES[i]);
            String next = i + 1 < SECTION_NAMES.length ? Pattern.quote(SECTION_NAMES[i + 1]) : "$";
            Pattern pattern = Pattern.compile(current + "[：:\\s]*([\\s\\S]*?)(?=" + next + "[：:\\s]*|$)");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                sections.put(SECTION_NAMES[i], matcher.group(1).trim());
            }
        }
        return sections;
    }

    private String value(Map<String, String> sections, String key, String fallback) {
        String value = sections.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "原文没有足够信息";
        }
        return text.lines().filter(line -> !line.isBlank()).findFirst().orElse(text);
    }
}
