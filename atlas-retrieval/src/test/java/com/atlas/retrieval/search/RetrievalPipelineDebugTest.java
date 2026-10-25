package com.atlas.retrieval.search;

import com.atlas.core.model.RetrievedContext;
import com.atlas.core.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manual debug test — runs against the real DB and real OpenAI.
 *
 * Run with:
 *   mvn test -pl atlas-retrieval -Dtest=RetrievalPipelineDebugTest -DOPENAI_API_KEY=... -DDB_URL=... -DDB_USERNAME=... -DDB_PASSWORD=...
 *
 * Or set env vars in your shell and run:
 *   mvn test -pl atlas-retrieval -Dtest=RetrievalPipelineDebugTest
 *
 * Not part of CI — excluded from the default surefire run via @Tag.
 */
@SpringBootTest
class RetrievalPipelineDebugTest {

    @Autowired
    private VectorSearchService vectorSearch;

    @Autowired
    private KeywordSearchService keywordSearch;

    @Autowired
    private HybridSearchService hybridSearch;

    @Autowired
    private RetrievalPipeline pipeline;

    // -----------------------------------------------------------------------
    // Change these queries to whatever you want to debug
    // -----------------------------------------------------------------------
    private static final String[] TEST_QUERIES = {
        "how does Spring AI handle embeddings",
        "ChatClient builder method",
        "VectorStore similarity search",
        "how to configure OpenAI API key in Spring Boot",
        "what is the difference between EmbeddingModel and ChatModel"
    };

    @Test
    void debugHybridSearch() {
        String query = TEST_QUERIES[0];
        int topK = 10;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("HYBRID SEARCH DEBUG");
        System.out.println("Query : " + query);
        System.out.println("Top-K : " + topK);
        System.out.println("=".repeat(80));

        List<SearchResult> fused = hybridSearch.search(query, topK, null);

        System.out.printf("%nFused results: %d chunks%n%n", fused.size());
        for (int i = 0; i < fused.size(); i++) {
            SearchResult r = fused.get(i);
            System.out.printf("[%2d] score=%.4f  tokens=%-4d  %s (chunk %d)%n",
                    i + 1, r.score(), r.tokenCount(), r.sourceFile(), r.chunkIndex());
            System.out.println("     " + truncate(r.content(), 120));
            System.out.println();
        }
    }

    @Test
    void debugRetrievalPipeline() {
        for (String query : TEST_QUERIES) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("RETRIEVAL PIPELINE DEBUG");
            System.out.println("Query: " + query);
            System.out.println("=".repeat(80));

            RetrievedContext ctx = pipeline.retrieve(query);

            System.out.printf("%nSelected %d chunks | %d / %d tokens used%n%n",
                    ctx.chunks().size(), ctx.totalTokens(), ctx.tokenBudget());

            for (int i = 0; i < ctx.chunks().size(); i++) {
                SearchResult r = ctx.chunks().get(i);
                System.out.printf("[%2d] score=%.4f  tokens=%-4d  %s (chunk %d)%n",
                        i + 1, r.score(), r.tokenCount(), r.sourceFile(), r.chunkIndex());
                System.out.println("     " + truncate(r.content(), 120));
                System.out.println();
            }
        }
    }

    @Test
    void debugSingleQueryAllStages() {
        String query = "how does Spring AI handle embeddings";

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ALL-STAGES DEBUG: " + query);
        System.out.println("=".repeat(80));

        // Stage 1a: vector only
        System.out.println("\n--- Stage 1a: VectorSearchService (top 5) ---");
        // HybridSearch runs both internally; here we call pipeline and compare

        // Stage 2+3+4: full pipeline
        RetrievedContext ctx = pipeline.retrieve(query, 20, null);

        System.out.printf("%nFinal context: %d chunks, %d tokens (budget: %d)%n",
                ctx.chunks().size(), ctx.totalTokens(), ctx.tokenBudget());

        System.out.println("\nFull content of selected chunks:");
        System.out.println("-".repeat(80));
        for (int i = 0; i < ctx.chunks().size(); i++) {
            SearchResult r = ctx.chunks().get(i);
            System.out.printf("%n[Chunk %d] score=%.4f | %s | chunk_index=%d | tokens=%d%n",
                    i + 1, r.score(), r.sourceFile(), r.chunkIndex(), r.tokenCount());
            System.out.println(r.content());
            System.out.println("-".repeat(80));
        }
    }

    @Test
    void compareStrategies() {
        int topK = 5;
        String version = "1.1.5";

        for (String query : TEST_QUERIES) {
            System.out.println("\n" + "=".repeat(90));
            System.out.println("QUERY: " + query);
            System.out.println("=".repeat(90));

            List<SearchResult> vectorResults  = vectorSearch.search(query, topK, version);
            List<SearchResult> keywordResults = keywordSearch.search(query, topK, version);
            List<SearchResult> hybridResults  = hybridSearch.search(query, topK, version);

            // Print each strategy's top-5 source files
            System.out.println("\n  VECTOR-ONLY (cosine similarity):");
            vectorResults.forEach(r -> System.out.printf("    [%.4f] %s (chunk %d)%n",
                    r.score(), shortPath(r.sourceFile()), r.chunkIndex()));

            System.out.println("\n  KEYWORD-ONLY (ts_rank_cd):");
            if (keywordResults.isEmpty()) {
                System.out.println("    (no results — query has no indexable lexemes)");
            } else {
                keywordResults.forEach(r -> System.out.printf("    [%.4f] %s (chunk %d)%n",
                        r.score(), shortPath(r.sourceFile()), r.chunkIndex()));
            }

            System.out.println("\n  HYBRID (RRF fusion):");
            hybridResults.forEach(r -> System.out.printf("    [%.4f] %s (chunk %d)%n",
                    r.score(), shortPath(r.sourceFile()), r.chunkIndex()));

            // Overlap analysis
            Set<String> vectorIds  = ids(vectorResults);
            Set<String> keywordIds = ids(keywordResults);
            Set<String> hybridIds  = ids(hybridResults);

            Set<String> onlyVector  = diff(vectorIds, keywordIds);
            Set<String> onlyKeyword = diff(keywordIds, vectorIds);
            Set<String> inBoth      = intersect(vectorIds, keywordIds);
            Set<String> hybridExtra = diff(hybridIds, union(vectorIds, keywordIds));

            System.out.println("\n  OVERLAP ANALYSIS:");
            System.out.println("    In both vector + keyword : " + inBoth.size());
            System.out.println("    Only in vector           : " + onlyVector.size());
            System.out.println("    Only in keyword          : " + onlyKeyword.size());
            System.out.println("    Hybrid adds new chunks   : " + hybridExtra.size());
        }

        System.out.println("\n" + "=".repeat(90));
        System.out.println("COMPARISON COMPLETE");
        System.out.println("=".repeat(90));
    }

    // ---- helpers ----

    private Set<String> ids(List<SearchResult> results) {
        return results.stream()
                .map(r -> r.id().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> diff(Set<String> a, Set<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }

    private Set<String> intersect(Set<String> a, Set<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private Set<String> union(Set<String> a, Set<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.addAll(b);
        return result;
    }

    private String shortPath(String path) {
        // Show just the filename, not the full antora path
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private String truncate(String text, int maxLen) {
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() <= maxLen ? oneLine : oneLine.substring(0, maxLen) + "...";
    }
}
