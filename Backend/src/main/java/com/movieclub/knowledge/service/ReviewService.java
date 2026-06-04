package com.movieclub.knowledge.service;

import com.movieclub.knowledge.dto.ReviewRequest;
import com.movieclub.knowledge.entity.ReviewReport;

public interface ReviewService {
    ReviewReport summarize(ReviewRequest request);
}
