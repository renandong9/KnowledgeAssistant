package com.movieclub.knowledge.service;

import com.movieclub.knowledge.dto.PaperRecommendRequest;
import com.movieclub.knowledge.entity.PaperRecommendation;

import java.util.List;

public interface PaperService {
    List<PaperRecommendation> recommend(PaperRecommendRequest request);

    List<PaperRecommendation> recommendForDocument(Long documentId);

    List<PaperRecommendation> listByDocument(Long documentId);
}
