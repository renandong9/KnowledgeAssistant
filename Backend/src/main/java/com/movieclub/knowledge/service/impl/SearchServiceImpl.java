package com.movieclub.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieclub.knowledge.ai.EmbeddingService;
import com.movieclub.knowledge.dto.SearchRequest;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.entity.DocumentChunk;
import com.movieclub.knowledge.mapper.DocumentChunkMapper;
import com.movieclub.knowledge.mapper.DocumentMapper;
import com.movieclub.knowledge.service.SearchService;
import com.movieclub.knowledge.vo.SearchResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    private final DocumentChunkMapper chunkMapper;
    private final DocumentMapper documentMapper;
    private final EmbeddingService embeddingService;

    public SearchServiceImpl(DocumentChunkMapper chunkMapper, DocumentMapper documentMapper, EmbeddingService embeddingService) {
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
        this.embeddingService = embeddingService;
    }

    @Override
    public List<SearchResult> search(SearchRequest request) {
        int limit = request.getLimit() == null ? 8 : request.getLimit();
        List<DocumentChunk> candidates = chunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                .like(DocumentChunk::getContent, request.getQuery())
                .last("LIMIT 50"));
        if (candidates.size() < limit) {
            candidates = chunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>().last("LIMIT 200"));
        }
        if (candidates.isEmpty()) {
            return List.of();
        }
        double[] queryVector = embeddingService.embed(request.getQuery());
        Map<Long, Document> documents = documentMapper.selectBatchIds(
                        candidates.stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Document::getId, document -> document));
        return candidates.stream()
                .map(chunk -> toResult(chunk, documents.get(chunk.getDocumentId()), queryVector))
                .sorted(Comparator.comparing(SearchResult::getScore).reversed())
                .limit(limit)
                .toList();
    }

    private SearchResult toResult(DocumentChunk chunk, Document document, double[] queryVector) {
        double vectorScore = embeddingService.cosine(queryVector, embeddingService.readEmbedding(chunk.getEmbedding()));
        double keywordBoost = chunk.getContent().contains(document == null ? "" : document.getTitle()) ? 0.05 : 0.0;
        return new SearchResult(
                chunk.getDocumentId(),
                chunk.getId(),
                document == null ? "未知文档" : document.getTitle(),
                chunk.getContent(),
                chunk.getPageNumber(),
                vectorScore + keywordBoost
        );
    }
}
