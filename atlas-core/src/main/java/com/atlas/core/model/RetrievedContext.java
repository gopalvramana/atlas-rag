package com.atlas.core.model;

import java.util.List;

public record RetrievedContext(
        List<SearchResult> chunks,
        int totalTokens,
        int tokenBudget
) {
    public boolean isWithinBudget() {
        return totalTokens <= tokenBudget;
    }
}
