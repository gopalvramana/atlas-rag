package com.atlas.retrieval.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlas.core.model.SearchResult;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);
    private static final int DEFAULT_TOP_K = 10;

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public VectorSearchService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SearchResult> search(String query) {
        return search(query, DEFAULT_TOP_K, null);
    }

    public List<SearchResult> search(String query, int topK, String version) {
        float[] queryEmbedding = embeddingModel.embed(query);
        String vectorString = toVectorString(queryEmbedding);

        String sql;
        Object[] params;

        if (version != null && !version.isBlank()) {
            sql = """
                SELECT id, content, source_file, version, chunk_index,
                       1 - (embedding <=> ?::vector) AS score
                FROM chunks
                WHERE version = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;
            params = new Object[]{vectorString, version, vectorString, topK};
        } else {
            sql = """
                SELECT id, content, source_file, version, chunk_index,
                       1 - (embedding <=> ?::vector) AS score
                FROM chunks
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;
            params = new Object[]{vectorString, vectorString, topK};
        }

        List<SearchResult> results = jdbcTemplate.query(sql, params, (rs, rowNum) ->
                new SearchResult(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("content"),
                        rs.getString("source_file"),
                        rs.getString("version"),
                        rs.getInt("chunk_index"),
                        rs.getDouble("score")
                ));

        log.info("Vector search for '{}' → {} results (top score: {})",
                truncate(query, 60),
                results.size(),
                results.isEmpty() ? "n/a" : String.format("%.4f", results.get(0).score()));

        return results;
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
