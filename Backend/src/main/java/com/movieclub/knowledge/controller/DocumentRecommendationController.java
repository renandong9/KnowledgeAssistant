package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.entity.PaperRecommendation;
import com.movieclub.knowledge.service.PaperService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/recommendations")
public class DocumentRecommendationController {
    private final PaperService paperService;

    public DocumentRecommendationController(PaperService paperService) {
        this.paperService = paperService;
    }

    @GetMapping
    public ApiResponse<List<PaperRecommendation>> list(@PathVariable Long documentId) {
        return ApiResponse.ok(paperService.listByDocument(documentId));
    }

    @PostMapping("/rebuild")
    public ApiResponse<List<PaperRecommendation>> rebuild(@PathVariable Long documentId) {
        return ApiResponse.ok(paperService.recommendForDocument(documentId));
    }
}
