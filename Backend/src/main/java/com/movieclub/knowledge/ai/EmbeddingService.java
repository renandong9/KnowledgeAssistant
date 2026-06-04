package com.movieclub.knowledge.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

@Service
public class EmbeddingService {
    private static final int DIMENSION = 64;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public double[] embed(String text) {
        double[] vector = new double[DIMENSION];
        String normalized = text == null ? "" : text.toLowerCase();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            byte[] digest = digest(token);
            for (int i = 0; i < digest.length; i++) {
                int index = Byte.toUnsignedInt(digest[i]) % DIMENSION;
                vector[index] += (i % 2 == 0) ? 1.0 : -1.0;
            }
        }
        normalize(vector);
        return vector;
    }

    public String embedAsJson(String text) {
        try {
            return objectMapper.writeValueAsString(embed(text));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize embedding", e);
        }
    }

    public double[] readEmbedding(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception e) {
            return new double[DIMENSION];
        }
    }

    public double cosine(double[] left, double[] right) {
        double dot = 0.0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
        }
        return dot;
    }

    private byte[] digest(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return token.getBytes(StandardCharsets.UTF_8);
        }
    }

    private void normalize(double[] vector) {
        double norm = Math.sqrt(Arrays.stream(vector).map(v -> v * v).sum());
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
