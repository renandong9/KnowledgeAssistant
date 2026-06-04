package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.dto.SearchRequest;
import com.movieclub.knowledge.service.SearchService;
import com.movieclub.knowledge.vo.SearchResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ApiResponse<List<SearchResult>> search(@Valid @RequestBody SearchRequest request) {
        return ApiResponse.ok(searchService.search(request));
    }
}
