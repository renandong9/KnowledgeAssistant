package com.movieclub.knowledge.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class PdfDocumentParser implements DocumentParser {
    @Override
    public boolean supports(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(Path path) throws Exception {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            String text = new PDFTextStripper().getText(document);
            return new ParsedDocument(text == null ? "" : text.trim());
        }
    }
}
