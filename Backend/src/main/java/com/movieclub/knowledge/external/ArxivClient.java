package com.movieclub.knowledge.external;

import com.movieclub.knowledge.entity.PaperRecommendation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ArxivClient {
    public List<PaperRecommendation> search(String query, int limit) {
        PaperRecommendation paper = new PaperRecommendation();
        paper.setSource("arXiv");
        paper.setQueryText(query);
        paper.setTitle("arXiv fallback search");
        paper.setAuthors("");
        paper.setAbstractText("Semantic Scholar 不可用时的降级占位结果。请稍后扩展 arXiv XML 解析。");
        paper.setUrl("https://arxiv.org/search/?query=" + query.replace(" ", "+") + "&searchtype=all");
        paper.setReason("Semantic Scholar 请求失败后的 arXiv 检索入口。");
        paper.setCreateTime(LocalDateTime.now());
        return List.of(paper);
    }
}
