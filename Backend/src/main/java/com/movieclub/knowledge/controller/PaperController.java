package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.dto.PaperRecommendRequest;
import com.movieclub.knowledge.entity.PaperRecommendation;
import com.movieclub.knowledge.service.PaperService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class PaperController {
    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping("/recommend")
    public ApiResponse<List<PaperRecommendation>> recommend(@Valid @RequestBody PaperRecommendRequest request) {
        return ApiResponse.ok(paperService.recommend(request));
    }
}
