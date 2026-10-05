# Atlas RAG — Build Progress

Read this at the start of every session. Update it at every commit checkpoint —
not at the end of a session. See `docs/PLAN.md` Section 7 for why this rule exists
(it slipped in the previous attempt at this project and cost us an accurate record).

---

## Current Status

| # | Module | Exit condition | Status | Commit |
|---|---|---|---|---|
| 1 | `infra` (Flyway migrations) | Migration applies cleanly; `\d chunks` shows correct schema | ✅ Done | |
| 2 | `atlas-core` | Domain model compiles; unit tests pass | ✅ Done | |
| 3 | `atlas-ingestion` | Chunks loaded into DB | ✅ Done | |
| 4 | `atlas-retrieval` (vector) | Vector search returns ranked results with score | ✅ Done | |
| 5 | `atlas-retrieval` (BM25 + hybrid) | Hybrid retrieval via BM25 + RRF verified | ⬜ Next | |
| 6 | `atlas-api` (basic) | `POST /api/v1/ask` returns valid structured JSON | ⬜ Not started | |
| 7 | `atlas-api` (streaming) | SSE token stream verified | ⬜ Not started | |
| 8 | `atlas-agent` | Agent calls `searchDocs` before answering | ⬜ Not started | |
| 9 | `atlas-evals` | 50-question dataset; CI job; <80% blocks merge | ⬜ Not started | |
| 10 | `atlas-mcp` | Claude Desktop calls `ask_atlas`, gets a response | ⬜ Not started | |

## ✅ Done

**Sprint 1 — Embedding Foundation + Ingestion (complete)**

- Step 1 — infra: V1 Flyway migration, `chunks` table with pgvector column, `atlas_rag` DB
- Step 2 — atlas-core: `Chunk` JPA entity with builder pattern, pgvector embedding column
- Step 3 — atlas-ingestion: Full pipeline built and verified end-to-end
  - `DocumentFetcher` / `GitHubDocumentFetcher` (multi-tag, lazy content loading)
  - `DocumentParser` / `AsciiDocParser` (AsciidoctorJ + Jsoup)
  - `DocumentChunker` / `SlidingWindowChunker` (512-token window, 64-token overlap, jtokkit cl100k_base)
  - `IngestionPipeline` orchestrator wiring fetch → parse → chunk → embed → store
  - Spring AI OpenAI embedding model (text-embedding-3-small, 1536 dims)
  - **1,260 chunks** loaded in DB across Spring AI docs versions
- Step 4 — atlas-retrieval (vector): `VectorSearchService` built and tested
  - Cosine similarity via pgvector `<=>` operator
  - Top-K with optional version filter
  - End-to-end search verified (score ~0.71 for test query)

## ⬜ Next — Sprint 3: Hybrid Retrieval

Goal: build the complete retrieval pipeline from query to final context block ready for the LLM.

```
Query
  ↓
Semantic Search (vector)   +   BM25 Search (full-text)
  ↓                                   ↓
         Merge — RRF
              ↓
           Rerank
              ↓
   Context Selection + Token-Aware Trimming
              ↓
     Final Context (ready for Sprint 4 RAG QA)
```

Steps:
1. Flyway V2 migration — add `tsvector` column to `chunks`
2. `BM25SearchService` — PostgreSQL full-text search (`ts_rank` / `to_tsquery`)
3. `HybridSearchService` — fuse vector + BM25 via RRF
4. `Reranker` — score-based reranking of fused results
5. `ContextSelector` — token-aware trimming to fit LLM context window
6. End-to-end comparison: vector-only vs BM25-only vs hybrid for 5 test queries

Sprint 2 (Knowledge Ingestion) maps to the ingestion pipeline already built in Step 3 above.
