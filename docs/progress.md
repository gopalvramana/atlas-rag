# Atlas RAG — Build Progress

Read this at the start of every session. Update it at every commit checkpoint —
not at the end of a session. See `docs/PLAN.md` Section 7 for why this rule exists
(it slipped in the previous attempt at this project and cost us an accurate record).

---

## Current Status

| # | Module | Exit condition | Status | Commit |
|---|---|---|---|---|
| 1 | `infra` (Flyway migrations) | Migration applies cleanly; `\d chunks` shows correct schema | ⬜ Not started | |
| 2 | `atlas-core` | Domain model compiles; unit tests pass | ⬜ Not started | |
| 3 | `atlas-ingestion` | Chunks loaded into DB (target: ~570 across 3 versions) | ⬜ Not started | |
| 4 | `atlas-retrieval` | Top-K retrieval correct for 10 hand-checked questions | ⬜ Not started | |
| 5 | `atlas-api` (basic) | `POST /api/v1/ask` returns valid structured JSON for 10 test questions | ⬜ Not started | |
| 6 | `atlas-api` (streaming) | SSE token stream verified | ⬜ Not started | |
| 7 | `atlas-agent` | Agent calls `searchDocs` before answering; `compileJavaSnippet` before returning code | ⬜ Not started | |
| 8 | Prompt caching | Cache hit/miss + latency delta logged | ⬜ Not started | |
| 9 | `atlas-evals` | 50-question dataset; CI job visible; <80% blocks merge | ⬜ Not started | |
| 10 | `atlas-mcp` | Claude Desktop calls `ask_atlas`, gets a response | ⬜ Not started | |

## ✅ Done

(nothing yet)

## ⬜ Next

Step 1 — infra. Design the Flyway migration before writing it: confirm the `chunks` /
`ingestion_runs` / `eval_runs` schema against `docs/PLAN.md` Section 2.2, then create
`V1__initial_schema.sql`.
