package com.atlas.retrieval;

import com.atlas.core.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);
    private static final int DEFAULT_TOP_K = 20;
    private static final int RRF_K = 60;

    // Virtual threads — ideal for I/O-bound JDBC calls on Java 21
    private static final ExecutorService executor =
            Executors.newVirtualThreadPerTaskExecutor();

    private final VectorSearchService vectorSearch;
    private final KeywordSearchService keywordSearch;

    public HybridSearchService(VectorSearchService vectorSearch,
                               KeywordSearchService keywordSearch) {
        this.vectorSearch = vectorSearch;
        this.keywordSearch = keywordSearch;
    }

    public List<SearchResult> search(String query) {
        return search(query, DEFAULT_TOP_K, null);
    }

    public List<SearchResult> search(String query, int topK, String version) {
        // Run both searches concurrently
        CompletableFuture<List<SearchResult>> vectorFuture =
                CompletableFuture.supplyAsync(
                        () -> vectorSearch.search(query, topK, version), executor);

        CompletableFuture<List<SearchResult>> keywordFuture =
                CompletableFuture.supplyAsync(
                        () -> keywordSearch.search(query, topK, version), executor);

        List<SearchResult> vectorResults = vectorFuture.join();
        List<SearchResult> keywordResults = keywordFuture.join();

        List<SearchResult> fused = rrf(vectorResults, keywordResults, topK);

        log.info("Hybrid search for '{}' → vector={}, keyword={}, fused={}",
                truncate(query, 60),
                vectorResults.size(),
                keywordResults.size(),
                fused.size());

        return fused;
    }

    private List<SearchResult> rrf(List<SearchResult> vectorResults,
                                   List<SearchResult> keywordResults,
                                   int topK) {
        Map<UUID, Double> rrfScores = new HashMap<>();
        Map<UUID, SearchResult> byId = new HashMap<>();

        // Accumulate RRF score from vector ranks
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult r = vectorResults.get(i);
            rrfScores.merge(r.id(), 1.0 / (RRF_K + i + 1), Double::sum);
            byId.putIfAbsent(r.id(), r);
        }

        // Accumulate RRF score from keyword ranks
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult r = keywordResults.get(i);
            rrfScores.merge(r.id(), 1.0 / (RRF_K + i + 1), Double::sum);
            byId.putIfAbsent(r.id(), r);
        }

        // Sort by RRF score descending, replace original score with RRF score
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    SearchResult original = byId.get(e.getKey());
                    return new SearchResult(
                            original.id(),
                            original.content(),
                            original.sourceFile(),
                            original.version(),
                            original.chunkIndex(),
                            e.getValue()
                    );
                })
                .toList();
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
