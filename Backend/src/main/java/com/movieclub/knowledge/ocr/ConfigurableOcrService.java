package com.movieclub.knowledge.ocr;

import com.movieclub.knowledge.config.KnowledgeProperties;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

@Service
public class ConfigurableOcrService implements OcrService {
    private final KnowledgeProperties properties;

    public ConfigurableOcrService(KnowledgeProperties properties) {
        this.properties = properties;
    }

    @Override
    public OcrResult recognize(Path filePath, String fileType) {
        if (!properties.getOcr().isEnabled()) {
            return OcrResult.skipped("OCR is disabled. Set OCR_ENABLED=true and configure Tesseract to handle scanned PDFs.");
        }
        if (!"pdf".equalsIgnoreCase(fileType)) {
            return OcrResult.skipped("OCR is only attempted for scanned PDF files.");
        }
        if (!"tess4j".equalsIgnoreCase(properties.getOcr().getProvider())) {
            return OcrResult.failed("Unsupported OCR provider: " + properties.getOcr().getProvider());
        }
        try {
            return recognizePdf(filePath);
        } catch (Exception e) {
            return OcrResult.failed("OCR failed. Ensure Tesseract is installed and TESSDATA_PREFIX/OCR_LANGUAGE are configured. "
                    + e.getMessage());
        }
    }

    private OcrResult recognizePdf(Path filePath) throws Exception {
        Tesseract tesseract = new Tesseract();
        if (StringUtils.hasText(properties.getOcr().getTesseractDataPath())) {
            tesseract.setDatapath(properties.getOcr().getTesseractDataPath());
        }
        if (StringUtils.hasText(properties.getOcr().getLanguage())) {
            tesseract.setLanguage(properties.getOcr().getLanguage());
        }

        int dpi = properties.getOcr().getDpi() <= 0 ? 200 : properties.getOcr().getDpi();
        int maxPages = properties.getOcr().getMaxPages() <= 0 ? 20 : properties.getOcr().getMaxPages();
        StringBuilder text = new StringBuilder();
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), maxPages);
            for (int page = 0; page < pages; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi, ImageType.RGB);
                text.append(runTesseract(tesseract, image, page)).append('\n');
            }
        }
        String result = text.toString().trim();
        if (!StringUtils.hasText(result)) {
            return OcrResult.failed("OCR completed but no text was recognized.");
        }
        return OcrResult.success(result);
    }

    private String runTesseract(Tesseract tesseract, BufferedImage image, int page) throws TesseractException {
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new TesseractException("page " + (page + 1) + ": " + e.getMessage());
        }
    }
}
