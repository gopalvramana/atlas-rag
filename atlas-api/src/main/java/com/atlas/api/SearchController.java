package com.atlas.api;

import com.atlas.core.model.SearchResult;
import com.atlas.retrieval.search.VectorSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SearchController {

    private final VectorSearchService vectorSearchService;

    public SearchController(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        int topK = request.topK() != null ? request.topK() : 10;
        List<SearchResult> results = vectorSearchService.search(request.query(), topK, request.version());

        return ResponseEntity.ok(new SearchResponse(request.query(), results.size(), results));
    }
}
