package com.atlas.retrieval.search;

import com.atlas.core.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class KeywordSearchService {

    private static final Logger log = LoggerFactory.getLogger(KeywordSearchService.class);
    private static final int DEFAULT_TOP_K = 10;

    private final JdbcTemplate jdbcTemplate;

    public KeywordSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SearchResult> search(String query) {
        return search(query, DEFAULT_TOP_K, null);
    }

    public List<SearchResult> search(String query, int topK, String version) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Guard: websearch_to_tsquery produces an empty tsquery for all-stopword input
        // (e.g. "the a is"). An empty tsquery matches nothing, but running the main
        // query is still wasteful. Detect by comparing against the empty tsquery literal.
        Boolean hasLexemes = jdbcTemplate.queryForObject(
                "SELECT websearch_to_tsquery('english', ?) <> ''::tsquery",
                Boolean.class,
                query);

        if (!Boolean.TRUE.equals(hasLexemes)) {
            log.debug("Keyword search for '{}' produced no lexemes — returning empty", query);
            return Collections.emptyList();
        }

        String sql;
        Object[] params;

        if (version != null && !version.isBlank()) {
            sql = """
                SELECT id, content, source_file, version, chunk_index,
                       ts_rank_cd(search_vector, query) AS score
                FROM chunks, websearch_to_tsquery('english', ?) query
                WHERE search_vector @@ query
                  AND version = ?
                ORDER BY score DESC
                LIMIT ?
                """;
            params = new Object[]{query, version, topK};
        } else {
            sql = """
                SELECT id, content, source_file, version, chunk_index,
                       ts_rank_cd(search_vector, query) AS score
                FROM chunks, websearch_to_tsquery('english', ?) query
                WHERE search_vector @@ query
                ORDER BY score DESC
                LIMIT ?
                """;
            params = new Object[]{query, topK};
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

        log.info("Keyword search for '{}' → {} results (top score: {})",
                truncate(query, 60),
                results.size(),
                results.isEmpty() ? "n/a" : String.format("%.4f", results.get(0).score()));

        return results;
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
