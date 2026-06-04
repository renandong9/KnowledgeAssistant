package com.movieclub.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieclub.knowledge.ai.EmbeddingService;
import com.movieclub.knowledge.common.BusinessException;
import com.movieclub.knowledge.config.KnowledgeProperties;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.entity.DocumentChunk;
import com.movieclub.knowledge.mapper.DocumentChunkMapper;
import com.movieclub.knowledge.mapper.DocumentMapper;
import com.movieclub.knowledge.parser.DocumentParser;
import com.movieclub.knowledge.parser.ParsedDocument;
import com.movieclub.knowledge.service.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {
    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 150;

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final List<DocumentParser> parsers;
    private final KnowledgeProperties properties;
    private final EmbeddingService embeddingService;

    public DocumentServiceImpl(DocumentMapper documentMapper,
                               DocumentChunkMapper chunkMapper,
                               List<DocumentParser> parsers,
                               KnowledgeProperties properties,
                               EmbeddingService embeddingService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.parsers = parsers;
        this.properties = properties;
        this.embeddingService = embeddingService;
    }

    @Override
    public Document upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String fileType = extension(originalFilename);
        DocumentParser parser = parsers.stream()
                .filter(candidate -> candidate.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new BusinessException("暂不支持的文件类型：" + fileType));
        Path savedPath = saveFile(file, fileType);
        LocalDateTime now = LocalDateTime.now();
        Document document = new Document();
        document.setTitle(stripExtension(originalFilename));
        document.setOriginalFilename(originalFilename);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setStoragePath(savedPath.toString());
        document.setParseStatus("PENDING");
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        documentMapper.insert(document);

        try {
            document.setParseStatus("PROCESSING");
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
            ParsedDocument parsed = parser.parse(savedPath);
            if (!StringUtils.hasText(parsed.content())) {
                throw new BusinessException("文档没有可提取文本，扫描版 PDF 可能需要 OCR");
            }
            List<String> chunks = split(parsed.content());
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunk.setPositionHint("chunk-" + i);
                chunk.setEmbedding(embeddingService.embedAsJson(chunks.get(i)));
                chunk.setTokenCount(estimateTokens(chunks.get(i)));
                chunk.setCreatedAt(LocalDateTime.now());
                chunkMapper.insert(chunk);
            }
            document.setSummary(parsed.content().substring(0, Math.min(parsed.content().length(), 500)));
            document.setParseStatus("COMPLETED");
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
            return document;
        } catch (Exception e) {
            chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, document.getId()));
            document.setParseStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
            throw new BusinessException("文档解析失败：" + e.getMessage());
        }
    }

    @Override
    public List<Document> list() {
        return documentMapper.selectList(new LambdaQueryWrapper<Document>().orderByDesc(Document::getCreatedAt));
    }

    @Override
    public Document getById(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("文档不存在：" + id);
        }
        return document;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        documentMapper.deleteById(id);
    }

    private Path saveFile(MultipartFile file, String fileType) {
        try {
            Path dir = Path.of(properties.getStorageDir()).toAbsolutePath();
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "." + fileType);
            file.transferTo(target);
            return target;
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
    }

    private List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end).trim());
            if (end == text.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private int estimateTokens(String content) {
        return Math.max(1, content.length() / 2);
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException("文件缺少扩展名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String stripExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "未命名文档";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
