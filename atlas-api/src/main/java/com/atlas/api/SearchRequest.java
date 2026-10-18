package com.atlas.api;

public record SearchRequest(
        String query,
        Integer topK,
        String version
) {}
