package com.atlas.api;

import com.atlas.core.model.SearchResult;

import java.util.List;

public record SearchResponse(
        String query,
        int totalResults,
        List<SearchResult> results
) {}
