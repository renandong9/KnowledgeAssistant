package com.movieclub.knowledge.ocr;

import java.nio.file.Path;

public interface OcrService {
    OcrResult recognize(Path filePath, String fileType);
}
