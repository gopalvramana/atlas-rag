package com.atlas.retrieval.search;

import com.atlas.core.model.RetrievedContext;
import com.atlas.core.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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

    private String truncate(String text, int maxLen) {
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() <= maxLen ? oneLine : oneLine.substring(0, maxLen) + "...";
    }
}
