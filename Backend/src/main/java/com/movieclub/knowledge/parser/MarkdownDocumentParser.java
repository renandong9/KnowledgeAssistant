package com.movieclub.knowledge.parser;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class MarkdownDocumentParser implements DocumentParser {
    @Override
    public boolean supports(String fileType) {
        return "md".equalsIgnoreCase(fileType) || "markdown".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(Path path) throws Exception {
        return new ParsedDocument(Files.readString(path, StandardCharsets.UTF_8).trim());
    }
}
