package com.movieclub.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieclub.knowledge.common.BusinessException;
import com.movieclub.knowledge.dto.PaperRecommendRequest;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.entity.PaperRecommendation;
import com.movieclub.knowledge.external.ArxivClient;
import com.movieclub.knowledge.external.SemanticScholarClient;
import com.movieclub.knowledge.mapper.DocumentMapper;
import com.movieclub.knowledge.mapper.PaperRecommendationMapper;
import com.movieclub.knowledge.service.PaperService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaperServiceImpl implements PaperService {
    private final SemanticScholarClient semanticScholarClient;
    private final ArxivClient arxivClient;
    private final PaperRecommendationMapper paperMapper;
    private final DocumentMapper documentMapper;

    public PaperServiceImpl(SemanticScholarClient semanticScholarClient,
                            ArxivClient arxivClient,
                            PaperRecommendationMapper paperMapper,
                            DocumentMapper documentMapper) {
        this.semanticScholarClient = semanticScholarClient;
        this.arxivClient = arxivClient;
        this.paperMapper = paperMapper;
        this.documentMapper = documentMapper;
    }

    @Override
    public List<PaperRecommendation> recommend(PaperRecommendRequest request) {
        int limit = request.getLimit() == null ? 5 : request.getLimit();
        List<PaperRecommendation> papers = searchExternal(request.getQuery(), limit);
        papers.forEach(paperMapper::insert);
        return papers;
    }

    @Override
    @Transactional
    public List<PaperRecommendation> recommendForDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在：" + documentId);
        }
        document.setRecommendationStatus("PROCESSING");
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
        try {
            paperMapper.delete(new LambdaQueryWrapper<PaperRecommendation>()
                    .eq(PaperRecommendation::getDocumentId, documentId));
            String query = buildQuery(document);
            List<PaperRecommendation> papers = searchExternal(query, 5);
            papers.forEach(paper -> {
                paper.setDocumentId(documentId);
                paperMapper.insert(paper);
            });
            document.setRecommendationStatus(papers.isEmpty() ? "UNAVAILABLE" : "COMPLETED");
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            return papers;
        } catch (Exception e) {
            document.setRecommendationStatus("FAILED");
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            return List.of();
        }
    }

    @Override
    public List<PaperRecommendation> listByDocument(Long documentId) {
        return paperMapper.selectList(new LambdaQueryWrapper<PaperRecommendation>()
                .eq(PaperRecommendation::getDocumentId, documentId)
                .orderByDesc(PaperRecommendation::getCreateTime));
    }

    private List<PaperRecommendation> searchExternal(String query, int limit) {
        try {
            return semanticScholarClient.search(query, limit);
        } catch (Exception e) {
            return arxivClient.search(query, limit);
        }
    }

    private String buildQuery(Document document) {
        return String.join(" ",
                nullToEmpty(document.getTitle()),
                nullToEmpty(document.getAbstractText()),
                nullToEmpty(document.getKeywords()),
                nullToEmpty(document.getSummary())).trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
