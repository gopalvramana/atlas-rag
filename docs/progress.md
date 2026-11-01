# Atlas RAG — Build Progress

Read this at the start of every session. Update it at every commit checkpoint —
not at the end of a session. See `docs/PLAN.md` Section 7 for why this rule exists.

---

## Sprint Map

| Sprint | Focus | Status |
|---|---|:---:|
| 1 | Embedding Foundation — vector DB, ingestion pipeline | ✅ Done |
| 2 | Knowledge Ingestion — fetch → parse → chunk → embed → store | ✅ Done |
| 3 | Hybrid Retrieval — keyword + vector + RRF + rerank + context selection | ✅ Done |
| 4 | RAG Question Answering — LLM generation with retrieved context | 🟡 In progress |
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
| Store | `IngestionPipeline` | Deduplicates by `(content_hash, version)`; writes to `chunks` table |
| Orchestrator | `IngestionPipeline` | Wires fetch → parse → chunk → embed → store |

**Verified:** **1,260 chunks** loaded — 516 at v1.0.0, 744 at v1.1.5

**Data integrity constraints:**
- `UNIQUE (content_hash, version)` — prevents duplicate content within a version
- `UNIQUE (source_file, chunk_index, version)` — prevents double-inserting same positional chunk (V3 migration)

### Sprint 3 — Hybrid Retrieval ✅

Full retrieval pipeline from query to token-budget-respecting context block.

```
Query
  ├── VectorSearchService  (cosine similarity via pgvector)
  └── KeywordSearchService (tsvector + ts_rank_cd via GIN index)
              ↓ concurrent CompletableFuture
         HybridSearchService (RRF fusion, k=60)
              ↓
           Reranker (re-embed query → cosine vs stored embeddings)
              ↓
        ContextSelector (greedy token-budget selection)
              ↓
       RetrievedContext → Sprint 4
```

| Step | Class | What it does | Status |
|---|---|---|:---:|
| 3.1 | `V2__add_search_vector.sql` | Adds `tsvector` column, GIN index, auto-update trigger | ✅ |
| 3.2 | `KeywordSearchService` | `websearch_to_tsquery` + `ts_rank_cd`; stopword guard | ✅ |
| 3.3 | `HybridSearchService` | Concurrent search + RRF fusion | ✅ |
| 3.4 | `Reranker` | Re-embed query; batch-fetch stored embeddings; cosine rerank | ✅ |
| 3.5 | `ContextSelector` + `RetrievalPipeline` | Token-budget greedy selection; pipeline orchestrator | ✅ |
| 3.6 | `compareStrategies` test | 5 queries: vector vs keyword vs hybrid documented | ✅ |

**Supporting classes:**
- `BoundedVirtualExecutor` + `RetrievalConfig` — Spring-managed virtual thread executor with semaphore admission control
- `RetrievedContext` — output record: chunks, totalTokens, tokenBudget
- `SearchResult` — moved to `atlas-core`; carries tokenCount from ingestion
- `ChunkRepository.findLatestVersion()` — auto-resolves latest version when none specified

**Key findings from Task 6 comparison:**
- Vector wins for conceptual queries ("difference between EmbeddingModel and ChatModel")
- Keyword wins for configuration/exact-identifier queries ("how to configure OpenAI API key")
- Hybrid consistently surfaces chunks missed by either strategy alone

---

## 🟡 Sprint 4 — RAG Question Answering (in progress)

Goal: take `RetrievedContext` from Sprint 3 and produce a grounded, cited LLM response.
The core challenge is LLM-side context management — assembling a prompt that fits within
the model's context window while reserving space for output and avoiding duplicate chunks.

```
RetrievedContext  →  PromptAssembler  →  Token Budget Check  →  ChatModel  →  RagResponse
```

### Steps

| # | What | Exit condition | Status |
|---|---|---|:---:|
| 4.1 | `PromptAssembler` — system prompt + question + chunks + output reserve | Token breakdown logged; correct chunks selected within budget | ⬜ |
| 4.2 | Context overflow handling — trim / reject | Overflow triggers trim; no chunks → structured "cannot answer" | ⬜ |
| 4.3 | `RagService` — orchestrates retrieval → prompt → LLM → response | End-to-end call returns answer + citations | ⬜ |
| 4.4 | Spring AI `ChatModel` integration | Real LLM call succeeds; token usage logged | ⬜ |
| 4.5 | Grounding + hallucination control | System prompt enforces context-only answers; verified on 3 out-of-scope questions | ⬜ |
| 4.6 | `POST /api/v1/ask` endpoint | Returns `{ answer, citations, tokenUsage }` for 10 test questions | ⬜ |

### Classes to build

| Class | Module | Done |
|---|---|:---:|
| `PromptAssembler` | `atlas-agent` | ⬜ |
| `RagService` | `atlas-agent` | ⬜ |
| `RagResponse` | `atlas-agent` | ⬜ |
| `AskController` | `atlas-api` | ⬜ |
