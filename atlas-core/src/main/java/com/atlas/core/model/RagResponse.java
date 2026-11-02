package com.atlas.core.model;

import java.util.List;

public record RagResponse(
        String answer,
        List<Citation> citations,
        TokenUsage tokenUsage) {

    public record Citation(String sourceFile, String version, int chunkIndex) {}

    public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}
}
