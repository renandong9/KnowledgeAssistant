package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.entity.DocumentChunk;
import com.movieclub.knowledge.mapper.DocumentChunkMapper;
import com.movieclub.knowledge.service.DocumentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final DocumentChunkMapper chunkMapper;

    public DocumentController(DocumentService documentService, DocumentChunkMapper chunkMapper) {
        this.documentService = documentService;
        this.chunkMapper = chunkMapper;
    }

    @PostMapping("/upload")
    public ApiResponse<Document> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(documentService.upload(file));
    }

    @GetMapping
    public ApiResponse<List<Document>> list() {
        return ApiResponse.ok(documentService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Document> detail(@PathVariable Long id) {
        return ApiResponse.ok(documentService.getById(id));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<Document> status(@PathVariable Long id) {
        return ApiResponse.ok(documentService.getById(id));
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<List<DocumentChunk>> chunks(@PathVariable Long id) {
        documentService.getById(id);
        return ApiResponse.ok(chunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, id)
                .orderByAsc(DocumentChunk::getChunkIndex)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok(null);
    }
}
