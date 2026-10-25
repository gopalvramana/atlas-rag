package com.atlas.retrieval.search;

import com.atlas.core.model.RetrievedContext;
import com.atlas.core.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin orchestrator: HybridSearchService → Reranker → ContextSelector → RetrievedContext.
 * Sprint 4's RagService calls this and receives a token-budget-respecting context block.
 */
@Service
public class RetrievalPipeline {

    private static final Logger log = LoggerFactory.getLogger(RetrievalPipeline.class);

    private final HybridSearchService hybridSearch;
    private final Reranker reranker;
    private final ContextSelector contextSelector;

    public RetrievalPipeline(
            HybridSearchService hybridSearch,
            Reranker reranker,
            ContextSelector contextSelector) {
        this.hybridSearch = hybridSearch;
        this.reranker = reranker;
        this.contextSelector = contextSelector;
    }

    public RetrievedContext retrieve(String query) {
        return retrieve(query, 10, null);
    }

    public RetrievedContext retrieve(String query, int topK, String version) {
        log.info("RetrievalPipeline: query='{}' topK={} version={}",
                truncate(query, 60), topK, version);

        List<SearchResult> fused = hybridSearch.search(query, topK, version);
        List<SearchResult> reranked = reranker.rerank(query, fused);
        RetrievedContext context = contextSelector.select(reranked);

        log.info("RetrievalPipeline: {} chunks → {} reranked → {} selected ({} tokens)",
                fused.size(), reranked.size(), context.chunks().size(), context.totalTokens());

        return context;
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
