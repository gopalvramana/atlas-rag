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
| 4 | RAG Question Answering — LLM generation with retrieved context | ✅ Done |
| 5 | Agentic AI — tool calling, ReAct loop | ✅ Done |
| 6 | Advanced AI Applications — memory, streaming, model routing | 🟡 In progress |
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

## Sprint 4 — RAG Question Answering ✅

```
Question → RetrievalPipeline → PromptAssembler → ChatModel (gpt-4o-mini) → RagResponse
```

Two-layer token budget:
- Sprint 3 retrieval budget: 6000 tokens for chunks
- Sprint 4 prompt budget: 128K window − system prompt − question − 1500 output reserve = chunk budget

| Step | What | Status |
|---|---|:---:|
| 4.1 | `PromptAssembler` — system + question + chunk budget + trim | ✅ |
| 4.2 | Context overflow — trim from bottom; empty → "cannot answer" | ✅ |
| 4.3 | `RagService` — retrieval → prompt → LLM → response | ✅ |
| 4.4 | Spring AI `ChatModel` integration (gpt-4o-mini, temp=0.1) | ✅ |
| 4.5 | Grounding — system prompt enforces context-only; verified | ✅ |
| 4.6 | `POST /api/v1/ask` → `{ answer, citations, tokenUsage }` | ✅ |
| 4.7 | 10 real questions tested | ✅ |

**Results: 8/10 answered correctly; 2 correct "cannot answer" (ReAct, EmbeddingModel vs ChatModel comparison — not in corpus). No hallucinations observed.**

| Class | Module |
|---|---|
| `RagResponse` | `atlas-core` |
| `PromptAssembler` | `atlas-agent` |
| `RagService` | `atlas-agent` |
| `AskController` | `atlas-api` |

---

## Sprint 5 — Agentic AI ✅

```
Question → AtlasAgent → ChatClient (ReAct loop)
                              ├── searchDocs   → RetrievalPipeline
                              ├── compileJava  → javax.tools.JavaCompiler
                              └── searchGithub → GitHub REST API
                         → AgentResponse { answer, toolsUsed }
```

Spring AI manages the full ReAct loop automatically. The model calls tools as needed
across multiple iterations; each tool result is fed back until the model returns a
final text answer.

| Step | What | Status |
|---|---|:---:|
| 5.1 | `SearchDocsTool` — wraps `RetrievalPipeline` as `@Tool` | ✅ |
| 5.2 | `CompileJavaTool` — in-process Java compilation via `javax.tools` | ✅ |
| 5.3 | `SearchGithubTool` — GitHub Issues/PR search via REST API | ✅ |
| 5.4 | `AtlasAgent` — `ChatClient` with `SimpleLoggerAdvisor`, ReAct loop | ✅ |
| 5.5 | `AgentController` — `POST /api/v1/agent` | ✅ |
| 5.6 | `RecordingToolCallback` — decorator records toolsUsed per request | ✅ |
| 5.7 | Three test questions verified (single-tool and multi-tool) | ✅ |

**Key observations from testing:**
- Model issues parallel tool calls in a single turn (searchDocs + searchGithub fired together)
- compileJava retried 3× on failure — model attempts self-correction
- toolsUsed correctly captures all iterations: `[searchDocs, searchGithub, compileJava, compileJava, compileJava]`
- 29K prompt tokens consumed for a 5-iteration multi-tool question (context grows each turn)

| Class | Module |
|---|---|
| `SearchDocsTool` | `atlas-agent` |
| `CompileJavaTool` | `atlas-agent` |
| `SearchGithubTool` | `atlas-agent` |
| `AtlasAgent` | `atlas-agent` |
| `RecordingToolCallback` | `atlas-agent` |
| `AgentResponse` | `atlas-core` |
| `AgentController` | `atlas-api` |

---

## Sprint 6 — Advanced AI Applications 🟡

Not started in code yet. Design-before-code discussion comes first.

| Step | What | Status |
|---|---|:---:|
| 6.1 | Streaming — `GET /api/v1/ask/stream`, SSE `Flux<String>`, final `event: done` carries full response (spec: `PLAN.md` §2.5) | ⬜ Next |
| 6.2 | Conversation memory — design TBD | ⬜ |
| 6.3 | Model routing — design TBD | ⬜ |

**Next step:** agree streaming design (e.g. `ChatClient.stream()` in `RagService`, how citations
and token usage arrive in the final `done` event) before writing code.

---

## Housekeeping

- 2026-11-23 — `PLAN.md` §3 tracker synced with actual progress (steps 1–5 and 7 marked done
  with commits; it had not been updated since the rebuild started).
