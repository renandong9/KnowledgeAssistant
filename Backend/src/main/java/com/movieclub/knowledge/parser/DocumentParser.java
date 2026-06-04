package com.movieclub.knowledge.parser;

import java.nio.file.Path;

public interface DocumentParser {
    boolean supports(String fileType);

    ParsedDocument parse(Path path) throws Exception;
}
