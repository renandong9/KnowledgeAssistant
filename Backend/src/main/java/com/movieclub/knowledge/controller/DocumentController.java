package com.movieclub.knowledge.controller;

import com.movieclub.knowledge.common.ApiResponse;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.service.DocumentService;
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

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok(null);
    }
}
