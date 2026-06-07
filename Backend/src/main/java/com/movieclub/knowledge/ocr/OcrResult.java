package com.movieclub.knowledge.ocr;

public record OcrResult(boolean attempted, boolean success, String text, String message) {
    public static OcrResult skipped(String message) {
        return new OcrResult(false, false, "", message);
    }

    public static OcrResult success(String text) {
        return new OcrResult(true, true, text == null ? "" : text, "");
    }

    public static OcrResult failed(String message) {
        return new OcrResult(true, false, "", message);
    }
}
