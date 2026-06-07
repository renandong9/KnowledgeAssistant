package com.movieclub.knowledge.service;

import com.movieclub.knowledge.entity.PaperAnalysis;

public interface PaperAnalysisService {
    PaperAnalysis analyze(Long documentId);

    PaperAnalysis getByDocument(Long documentId);
}
