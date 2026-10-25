package com.atlas.retrieval.search;

import com.atlas.core.model.RetrievedContext;
import com.atlas.core.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy token-budget selection: iterate reranked candidates in score order,
 * add each chunk until the budget is exhausted. Uses the token_count stored
 * at ingestion time — no tokenizer call needed at query time.
 */
@Service
public class ContextSelector {

    private static final Logger log = LoggerFactory.getLogger(ContextSelector.class);

    private final int tokenBudget;

    public ContextSelector(
            @Value("${atlas.retrieval.token-budget:6000}") int tokenBudget) {
        this.tokenBudget = tokenBudget;
    }

    public RetrievedContext select(List<SearchResult> reranked) {
        return select(reranked, tokenBudget);
    }

    public RetrievedContext select(List<SearchResult> reranked, int budget) {
        List<SearchResult> selected = new ArrayList<>();
        int consumed = 0;

        for (SearchResult chunk : reranked) {
            int chunkTokens = chunk.tokenCount();
            if (consumed + chunkTokens > budget) {
                continue;
            }
            selected.add(chunk);
            consumed += chunkTokens;
        }

        log.info("ContextSelector: {}/{} chunks selected, {}/{} tokens used",
                selected.size(), reranked.size(), consumed, budget);

        return new RetrievedContext(selected, consumed, budget);
    }
}
