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

## ⬜ Next — Sprint 2: Hybrid Retrieval

Goal: add BM25 full-text search and fuse with vector results via Reciprocal Rank Fusion (RRF).

Steps:
1. Add `tsvector` column to `chunks` table (Flyway V2 migration)
2. Populate `tsvector` on insert (trigger or application-side)
3. Build `BM25SearchService` using PostgreSQL `ts_rank` / `to_tsquery`
4. Build `HybridSearchService` fusing vector + BM25 results with RRF
5. Compare: vector-only vs BM25-only vs hybrid for 5 test queries
6. Wire into `atlas-api`
