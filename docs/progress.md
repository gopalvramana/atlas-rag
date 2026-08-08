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
| 3 | `atlas-ingestion` | Chunks loaded into DB (target: ~570 across 3 versions) | 🟡 In progress | |
| 4 | `atlas-retrieval` | Top-K retrieval correct for 10 hand-checked questions | ⬜ Not started | |
| 5 | `atlas-api` (basic) | `POST /api/v1/ask` returns valid structured JSON for 10 test questions | ⬜ Not started | |
| 6 | `atlas-api` (streaming) | SSE token stream verified | ⬜ Not started | |
| 7 | `atlas-agent` | Agent calls `searchDocs` before answering; `compileJavaSnippet` before returning code | ⬜ Not started | |
| 8 | Prompt caching | Cache hit/miss + latency delta logged | ⬜ Not started | |
| 9 | `atlas-evals` | 50-question dataset; CI job visible; <80% blocks merge | ⬜ Not started | |
| 10 | `atlas-mcp` | Claude Desktop calls `ask_atlas`, gets a response | ⬜ Not started | |

## ✅ Done

- Step 1 — infra: V1 Flyway migration (chunks table only, no ingestion_runs/eval_runs yet), atlas_rag database created
- Step 2 — atlas-core: Chunk JPA entity with builder pattern, pgvector embedding column

## 🟡 In Progress

Step 3 — atlas-ingestion: DocumentFetcher/DocumentParser interfaces built, GitHubDocumentFetcher (multi-tag, lazy content loading), AsciiDocParser (AsciidoctorJ + Jsoup), Spring AI OpenAI embedding model configured and verified. Next: chunker, then pipeline orchestrator to wire fetch → parse → chunk → embed → store.
