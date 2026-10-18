package com.atlas.retrieval.search;

import com.atlas.core.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class Reranker {

    private static final Logger log = LoggerFactory.getLogger(Reranker.class);

    private final EmbeddingModel embeddingModel;
    private final NamedParameterJdbcTemplate namedJdbc;

    public Reranker(EmbeddingModel embeddingModel, NamedParameterJdbcTemplate namedJdbc) {
        this.embeddingModel = embeddingModel;
        this.namedJdbc = namedJdbc;
    }

    /**
     * Re-scores each candidate by computing fresh cosine similarity between
     * the query embedding and each chunk's stored embedding.
     *
     * Why re-embed instead of reusing retrieval scores:
     * - Keyword-only candidates never had a vector score — cosine gives them one
     * - Every candidate gets a consistent, comparable relevance score
     * - Upgrade path: replace cosine impl with Cohere Rerank API, same signature
     */
    public List<SearchResult> rerank(String query, List<SearchResult> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        // One embedding API call for the query
        float[] queryEmbedding = embeddingModel.embed(query);

        // One DB round trip to fetch all candidate embeddings
        List<UUID> ids = candidates.stream().map(SearchResult::id).toList();
        Map<UUID, float[]> embeddingById = fetchEmbeddings(ids);

        // Re-score and re-sort
        List<SearchResult> reranked = candidates.stream()
                .filter(r -> embeddingById.containsKey(r.id()))
                .map(r -> new SearchResult(
                        r.id(),
                        r.content(),
                        r.sourceFile(),
                        r.version(),
                        r.chunkIndex(),
                        cosineSimilarity(queryEmbedding, embeddingById.get(r.id()))
                ))
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .toList();

        log.info("Reranked {} candidates (top score: {})",
                reranked.size(),
                reranked.isEmpty() ? "n/a" : String.format("%.4f", reranked.get(0).score()));

        return reranked;
    }

    private Map<UUID, float[]> fetchEmbeddings(List<UUID> ids) {
        // Cast to text so JDBC receives the vector as a parseable string
        String sql = "SELECT id, embedding::text FROM chunks WHERE id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);

        Map<UUID, float[]> result = new HashMap<>();
        namedJdbc.query(sql, params, rs -> {
            UUID id = UUID.fromString(rs.getString("id"));
            float[] embedding = parseVector(rs.getString("embedding"));
            result.put(id, embedding);
        });
        return result;
    }

    // PostgreSQL returns vector as "[0.1,0.2,0.3,...]"
    private float[] parseVector(String vectorString) {
        String stripped = vectorString.substring(1, vectorString.length() - 1);
        String[] parts = stripped.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
