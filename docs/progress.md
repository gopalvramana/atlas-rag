# Atlas RAG — Build Progress

Read this at the start of every session. Update it at every commit checkpoint —
not at the end of a session. See `docs/PLAN.md` Section 7 for why this rule exists.

---

## Sprint Map

| Sprint | Focus | Status |
|---|---|:---:|
| 1 | Embedding Foundation — vector DB, ingestion pipeline | ✅ Done |
| 2 | Knowledge Ingestion — fetch → parse → chunk → embed → store | ✅ Done |
| 3 | Hybrid Retrieval — BM25 + vector + RRF + rerank + context selection | 🟡 In progress |
| 4 | RAG Question Answering — LLM generation with retrieved context | ⬜ Not started |
| 5 | Agentic AI — tool calling, ReAct loop | ⬜ Not started |
| 6 | Advanced AI Applications — memory, streaming, model routing | ⬜ Not started |
| 7 | Evaluation + Observability | ⬜ Not started |
| 8 | MCP Integration | ⬜ Not started |
| 9 | Enterprise Production AI Architecture | ⬜ Not started |

---

## What Is Built and Working

### Sprint 1 — Embedding Foundation ✅

**infra**
- `atlas_rag` PostgreSQL database running in Docker (`roms-postgres` container)
- Flyway V1 migration: `chunks` table with `id`, `content`, `source_file`, `version`,
  `chunk_index`, `embedding vector(1536)`, `created_at`
- pgvector extension enabled

**atlas-core**
- `Chunk` JPA entity — builder pattern, maps to `chunks` table
- `ChunkRepository` — Spring Data JPA

### Sprint 2 — Knowledge Ingestion ✅

**atlas-ingestion** — all components built, pipeline runs end-to-end

| Component | Class | What it does |
|---|---|---|
| Fetcher | `GitHubDocumentFetcher` | Lists files across multiple Spring AI doc versions via GitHub API; fetches content lazily |
| Parser | `AsciiDocParser` | Converts AsciiDoc → plain text using AsciidoctorJ + Jsoup; strips markup |
| Chunker | `SlidingWindowChunker` | 512-token windows, 64-token overlap, jtokkit cl100k_base tokenizer |
| Embedding | Spring AI `EmbeddingModel` | `text-embedding-3-small`, 1536 dimensions |
| Store | Spring AI `VectorStore` (pgvector) | Writes vectors to `chunks` table |
| Orchestrator | `IngestionPipeline` | Wires fetch → parse → chunk → embed → store |

**Verified:** **1,260 chunks** loaded in `atlas_rag.chunks`

**atlas-retrieval (vector search)** — built and tested

| Component | Class | What it does |
|---|---|---|
| Vector search | `VectorSearchService` | Embeds the query, runs cosine similarity (`<=>`) against `chunks.embedding`, returns Top-K with score |
| Result type | `SearchResult` | `id`, `content`, `source_file`, `version`, `chunk_index`, `score` |

**Verified:** End-to-end search working. Test query returned score ~0.71.

---

## 🟡 Sprint 3 — Hybrid Retrieval (in progress)

Goal: build the complete retrieval pipeline from query to a trimmed context block
ready for the LLM in Sprint 4.

```
Query
  ├── Semantic Search (vector / cosine)
  └── BM25 Search    (full-text / tsvector)
              ↓
         Merge via RRF
              ↓
           Rerank
              ↓
  Context Selection + Token-Aware Trimming
              ↓
     RetrievedContext  →  Sprint 4
```

### Steps

| # | What | Exit condition | Status |
|---|---|---|:---:|
| 3.1 | Flyway V2 migration — add `search_vector tsvector` + GIN index + trigger | `\d chunks` shows `search_vector`; `SELECT count(*) WHERE search_vector IS NOT NULL` = 1260 | ⬜ |
| 3.2 | `BM25SearchService` — `to_tsquery` + `ts_rank` | Returns ranked results for keyword query; verified manually | ⬜ |
| 3.3 | `HybridSearchService` — RRF fusion of vector + BM25 | Returns merged list; chunks in both lists rank higher than single-source chunks | ⬜ |
| 3.4 | `Reranker` — score-weighted reordering of fused candidates | Top result is more relevant than either vector-only or BM25-only alone | ⬜ |
| 3.5 | `ContextSelector` — token-aware trimming | Output fits within configured token budget; verified with jtokkit token count | ⬜ |
| 3.6 | End-to-end comparison | 5 test queries with vector-only vs BM25-only vs hybrid documented | ⬜ |

### Classes to build

| Class | Module | Done |
|---|---|:---:|
| `V2__add_search_vector.sql` | `atlas-ingestion` (Flyway) | ⬜ |
| `BM25SearchService` | `atlas-retrieval` | ⬜ |
| `HybridSearchService` | `atlas-retrieval` | ⬜ |
| `Reranker` | `atlas-retrieval` | ⬜ |
| `ContextSelector` | `atlas-retrieval` | ⬜ |
| `RetrievedContext` | `atlas-retrieval` | ⬜ |
