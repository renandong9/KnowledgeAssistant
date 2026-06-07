package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.entity.PaperAnalysis;
import com.movieclub.knowledge.service.PaperAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents/{documentId}/analysis")
public class PaperAnalysisController {
    private final PaperAnalysisService paperAnalysisService;

    public PaperAnalysisController(PaperAnalysisService paperAnalysisService) {
        this.paperAnalysisService = paperAnalysisService;
    }

    @GetMapping
    public ApiResponse<PaperAnalysis> get(@PathVariable Long documentId) {
        return ApiResponse.ok(paperAnalysisService.getByDocument(documentId));
    }

    @PostMapping("/rebuild")
    public ApiResponse<PaperAnalysis> rebuild(@PathVariable Long documentId) {
        return ApiResponse.ok(paperAnalysisService.analyze(documentId));
    }
}
