package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.dto.ReviewRequest;
import com.movieclub.knowledge.entity.ReviewReport;
import com.movieclub.knowledge.service.ReviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/summary")
    public ApiResponse<ReviewReport> summarize(@RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.summarize(request));
    }
}
