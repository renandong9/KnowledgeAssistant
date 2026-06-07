package com.movieclub.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieclub.knowledge.ai.EmbeddingService;
import com.movieclub.knowledge.common.BusinessException;
import com.movieclub.knowledge.config.KnowledgeProperties;
import com.movieclub.knowledge.entity.Document;
import com.movieclub.knowledge.entity.DocumentChunk;
import com.movieclub.knowledge.mapper.DocumentChunkMapper;
import com.movieclub.knowledge.mapper.DocumentMapper;
import com.movieclub.knowledge.ocr.OcrResult;
import com.movieclub.knowledge.ocr.OcrService;
import com.movieclub.knowledge.parser.DocumentParser;
import com.movieclub.knowledge.parser.ParsedDocument;
import com.movieclub.knowledge.service.DocumentService;
import com.movieclub.knowledge.service.PaperAnalysisService;
import com.movieclub.knowledge.service.PaperService;
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
    private final OcrService ocrService;
    private final PaperAnalysisService paperAnalysisService;
    private final PaperService paperService;

    public DocumentServiceImpl(DocumentMapper documentMapper,
                               DocumentChunkMapper chunkMapper,
                               List<DocumentParser> parsers,
                               KnowledgeProperties properties,
                               EmbeddingService embeddingService,
                               OcrService ocrService,
                               PaperAnalysisService paperAnalysisService,
                               PaperService paperService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.parsers = parsers;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.ocrService = ocrService;
        this.paperAnalysisService = paperAnalysisService;
        this.paperService = paperService;
    }

    @Override
    @Transactional
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
        Document document = createPendingDocument(file, originalFilename, fileType, savedPath);

        try {
            updateStatuses(document, "PROCESSING", "PENDING", "PENDING", "PENDING", null);
            ParsedDocument parsed = parser.parse(savedPath);
            String content = parsed.content();
            if (!StringUtils.hasText(content)) {
                OcrResult ocrResult = ocrService.recognize(savedPath, fileType);
                if (ocrResult.success() && StringUtils.hasText(ocrResult.text())) {
                    content = ocrResult.text();
                    document.setOcrStatus("COMPLETED");
                } else if (ocrResult.attempted()) {
                    document.setOcrStatus("FAILED");
                    throw new BusinessException("扫描版 PDF OCR 失败：" + ocrResult.message());
                } else {
                    document.setOcrStatus("SKIPPED");
                    throw new BusinessException("文档没有可提取文本；扫描版 PDF 需要配置 OCR。");
                }
            } else {
                document.setOcrStatus("NOT_REQUIRED");
            }
            persistChunks(document, content);
            document.setTitle(extractTitle(document, content));
            document.setAbstractText(extractAbstract(content));
            document.setKeywords(extractKeywords(content));
            document.setSummary(content.substring(0, Math.min(content.length(), 500)));
            updateStatuses(document, "COMPLETED", document.getOcrStatus(), "COMPLETED", "PROCESSING", null);
            runPostProcessing(document.getId());
            return documentMapper.selectById(document.getId());
        } catch (Exception e) {
            chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, document.getId()));
            updateStatuses(document, "FAILED", document.getOcrStatus(), "FAILED", "FAILED", e.getMessage());
            throw new BusinessException("文档处理失败：" + e.getMessage());
        }
    }

    @Override
    public List<Document> list() {
        return documentMapper.selectList(new LambdaQueryWrapper<Document>().orderByDesc(Document::getCreateTime));
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

    private Document createPendingDocument(MultipartFile file, String originalFilename, String fileType, Path savedPath) {
        LocalDateTime now = LocalDateTime.now();
        Document document = new Document();
        document.setTitle(stripExtension(originalFilename));
        document.setOriginalFileName(originalFilename);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setFilePath(savedPath.toString());
        document.setParseStatus("PENDING");
        document.setOcrStatus("PENDING");
        document.setIndexStatus("PENDING");
        document.setAnalysisStatus("PENDING");
        document.setRecommendationStatus("PENDING");
        document.setCreateTime(now);
        document.setUpdateTime(now);
        documentMapper.insert(document);
        return document;
    }

    private void persistChunks(Document document, String content) {
        document.setIndexStatus("PROCESSING");
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
        List<String> chunks = split(content);
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setPositionHint("chunk-" + i);
            chunk.setEmbedding(embeddingService.embedAsJson(chunks.get(i)));
            chunk.setTokenCount(estimateTokens(chunks.get(i)));
            chunk.setCreateTime(LocalDateTime.now());
            chunkMapper.insert(chunk);
        }
    }

    private void runPostProcessing(Long documentId) {
        try {
            paperAnalysisService.analyze(documentId);
        } catch (Exception ignored) {
            // Analysis status is stored by PaperAnalysisService.
        }
        try {
            paperService.recommendForDocument(documentId);
        } catch (Exception ignored) {
            Document document = documentMapper.selectById(documentId);
            if (document != null) {
                document.setRecommendationStatus("FAILED");
                document.setUpdateTime(LocalDateTime.now());
                documentMapper.updateById(document);
            }
        }
    }

    private void updateStatuses(Document document,
                                String parseStatus,
                                String ocrStatus,
                                String indexStatus,
                                String recommendationStatus,
                                String errorMessage) {
        document.setParseStatus(parseStatus);
        if (StringUtils.hasText(ocrStatus)) {
            document.setOcrStatus(ocrStatus);
        }
        document.setIndexStatus(indexStatus);
        document.setRecommendationStatus(recommendationStatus);
        document.setErrorMessage(errorMessage);
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
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

    private String extractTitle(Document document, String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> line.length() <= 120)
                .findFirst()
                .orElse(document.getTitle());
    }

    private String extractAbstract(String content) {
        String lower = content.toLowerCase();
        int index = lower.indexOf("abstract");
        if (index < 0) {
            return content.substring(0, Math.min(content.length(), 800));
        }
        return content.substring(index, Math.min(content.length(), index + 1000));
    }

    private String extractKeywords(String content) {
        return content.lines()
                .filter(line -> line.toLowerCase().contains("keywords") || line.contains("关键词"))
                .findFirst()
                .orElse("");
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
