package com.atlas.retrieval.search;

import com.atlas.core.model.RetrievedContext;
import com.atlas.core.model.SearchResult;
import com.atlas.core.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin orchestrator: HybridSearchService → Reranker → ContextSelector → RetrievedContext.
 * Sprint 4's RagService calls this and receives a token-budget-respecting context block.
 *
 * When no version is supplied, it resolves to the latest version in the DB so that
 * queries never accidentally mix chunks across different ingested versions.
 */
@Service
public class RetrievalPipeline {

    private static final Logger log = LoggerFactory.getLogger(RetrievalPipeline.class);

    private final HybridSearchService hybridSearch;
    private final Reranker reranker;
    private final ContextSelector contextSelector;
    private final ChunkRepository chunkRepository;

    public RetrievalPipeline(
            HybridSearchService hybridSearch,
            Reranker reranker,
            ContextSelector contextSelector,
            ChunkRepository chunkRepository) {
        this.hybridSearch = hybridSearch;
        this.reranker = reranker;
        this.contextSelector = contextSelector;
        this.chunkRepository = chunkRepository;
    }

    public RetrievedContext retrieve(String query) {
        return retrieve(query, 10, null);
    }

    public RetrievedContext retrieve(String query, int topK, String version) {
        String resolvedVersion = resolveVersion(version);
        log.info("RetrievalPipeline: query='{}' topK={} version={}",
                truncate(query, 60), topK, resolvedVersion);

        List<SearchResult> fused = hybridSearch.search(query, topK, resolvedVersion);
        List<SearchResult> reranked = reranker.rerank(query, fused);
        RetrievedContext context = contextSelector.select(reranked);

        log.info("RetrievalPipeline: {} chunks → {} reranked → {} selected ({} tokens)",
                fused.size(), reranked.size(), context.chunks().size(), context.totalTokens());

        return context;
    }

    private String resolveVersion(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        return chunkRepository.findLatestVersion()
                .orElseThrow(() -> new IllegalStateException(
                        "No chunks in the database — run ingestion first"));
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
