package com.movieclub.knowledge.service;

import com.movieclub.knowledge.dto.SearchRequest;
import com.movieclub.knowledge.vo.SearchResult;

import java.util.List;

public interface SearchService {
    List<SearchResult> search(SearchRequest request);
}
