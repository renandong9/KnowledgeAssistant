package com.movieclub.knowledge.external;

import com.movieclub.knowledge.config.KnowledgeProperties;
import com.movieclub.knowledge.entity.PaperRecommendation;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SemanticScholarClient {
    private final WebClient webClient;

    public SemanticScholarClient(KnowledgeProperties properties) {
        this.webClient = WebClient.builder().baseUrl(properties.getPaper().getSemanticScholarUrl()).build();
    }

    public List<PaperRecommendation> search(String query, int limit) {
        Map<?, ?> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/paper/search")
                        .queryParam("query", query)
                        .queryParam("limit", limit)
                        .queryParam("fields", "title,authors,year,abstract,url")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        Object dataObject = response == null ? null : response.get("data");
        List<?> data = dataObject instanceof List<?> rows ? rows : List.of();
        List<PaperRecommendation> papers = new ArrayList<>();
        for (Object item : data) {
            Map<?, ?> row = (Map<?, ?>) item;
            PaperRecommendation paper = new PaperRecommendation();
            paper.setSource("SemanticScholar");
            paper.setQueryText(query);
            paper.setTitle(String.valueOf(value(row, "title")));
            Object year = row.get("year");
            paper.setPublishedYear(year instanceof Number number ? number.intValue() : null);
            paper.setAbstractText(String.valueOf(value(row, "abstract")));
            paper.setUrl(String.valueOf(value(row, "url")));
            Object authorsObject = row.get("authors");
            List<?> authors = authorsObject instanceof List<?> list ? list : List.of();
            paper.setAuthors(authors.stream()
                    .filter(Map.class::isInstance)
                    .map(author -> String.valueOf(value((Map<?, ?>) author, "name")))
                    .collect(Collectors.joining(", ")));
            paper.setReason("与检索主题相关：" + query);
            paper.setCreatedAt(LocalDateTime.now());
            papers.add(paper);
        }
        return papers;
    }

    private Object value(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value;
    }
}
