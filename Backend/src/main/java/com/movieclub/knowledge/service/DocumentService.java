package com.movieclub.knowledge.service;

import com.movieclub.knowledge.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    Document upload(MultipartFile file);

    List<Document> list();

    Document getById(Long id);

    void delete(Long id);
}
