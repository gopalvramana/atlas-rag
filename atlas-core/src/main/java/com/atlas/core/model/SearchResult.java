package com.atlas.core.model;

import java.util.UUID;

public record SearchResult(
        UUID id,
        String content,
        String sourceFile,
        String version,
        int chunkIndex,
        double score,
        int tokenCount
) {}
