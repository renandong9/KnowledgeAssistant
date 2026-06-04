package com.movieclub.knowledge.service.impl;

import com.movieclub.knowledge.dto.PaperRecommendRequest;
import com.movieclub.knowledge.entity.PaperRecommendation;
import com.movieclub.knowledge.external.ArxivClient;
import com.movieclub.knowledge.external.SemanticScholarClient;
import com.movieclub.knowledge.mapper.PaperRecommendationMapper;
import com.movieclub.knowledge.service.PaperService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaperServiceImpl implements PaperService {
    private final SemanticScholarClient semanticScholarClient;
    private final ArxivClient arxivClient;
    private final PaperRecommendationMapper paperMapper;

    public PaperServiceImpl(SemanticScholarClient semanticScholarClient,
                            ArxivClient arxivClient,
                            PaperRecommendationMapper paperMapper) {
        this.semanticScholarClient = semanticScholarClient;
        this.arxivClient = arxivClient;
        this.paperMapper = paperMapper;
    }

    @Override
    public List<PaperRecommendation> recommend(PaperRecommendRequest request) {
        int limit = request.getLimit() == null ? 5 : request.getLimit();
        List<PaperRecommendation> papers;
        try {
            papers = semanticScholarClient.search(request.getQuery(), limit);
        } catch (Exception e) {
            papers = arxivClient.search(request.getQuery(), limit);
        }
        papers.forEach(paperMapper::insert);
        return papers;
    }
}
