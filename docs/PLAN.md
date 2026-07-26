# Atlas — Rebuild Master Spec

**Purpose of this document:** This is the single source of truth for rebuilding Atlas from scratch.
It exists outside the git repo on purpose — it must survive the GitHub repo delete/recreate.
Once the new repo exists, copy this file in as `docs/PLAN.md` (or keep it here and link to it).

It answers three things at every point in the rebuild:
1. What is the target architecture? (Section 2)
2. What is done vs. not done, right now? (Section 3 — **update this checklist at every commit**)
3. What do I need to be able to say in an interview about each piece? (Section 6)

---

## 1. The Pitch (say this in an interview, 30 seconds)

Atlas is a Q&A service over Spring AI's documentation. It's a personal engineering project
built to go deep on one AI-engineering technique per module, from scratch, in Java — not
through a framework that hides the mechanics:

- **Hybrid retrieval** — BM25 + vector search + Reciprocal Rank Fusion + cross-encoder reranking
- **ReAct agent loop** — tool calling, multi-step reasoning
- **Prompt caching** — measured latency/cost impact
- **Structured output** — schema-enforced LLM responses
- **Streaming** — SSE token-by-token
- **Evals as a CI gate** — an eval suite that blocks merges below a pass-rate threshold
- **MCP server** — expose the whole thing as a tool Claude Desktop can call

Why Spring AI docs as the corpus: version-controlled, well-structured `.adoc` source,
real breaking changes across versions (1.0-GA → 1.1 → 2.0-M) — genuine multi-version
retrieval problems, not synthetic ones.

---

## 2. Target Architecture

### 2.1 Repository structure

```
atlas/
├── atlas-core/               # Shared domain model
├── atlas-ingestion/          # CLI: fetch → extract → chunk → embed → store
├── atlas-retrieval/          # Hybrid search: BM25 + pgvector + RRF + rerank
├── atlas-agent/              # ReAct agent, tool calling, prompt building/caching
├── atlas-api/                # Runnable Spring Boot app: REST + SSE
├── atlas-mcp/                # Runnable MCP server: stdio transport
├── atlas-evals/              # Eval dataset + CI runner
├── infra/                    # Flyway migrations
└── docs/                     # ARCHITECTURE.md, decisions.md, progress.md, interview-prep.md
```

Dependency graph:
```
atlas-core
    ├── atlas-ingestion
    ├── atlas-retrieval
    │       └── atlas-agent
    │               ├── atlas-api      (runnable)
    │               ├── atlas-mcp      (runnable)
    │               └── atlas-evals
```

### 2.2 Data model (Flyway-versioned from day one — this was a v1 improvement over the original hand-written schema.sql, keep it)

```sql
-- chunks: the ingested knowledge base
CREATE TABLE chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source       TEXT NOT NULL,        -- 'SPRING_AI_GITHUB'
    version      TEXT NOT NULL,        -- '1.0-GA' | '1.1' | '2.0-M'
    section      TEXT,                 -- derived from filename, e.g. 'chatclient'
    url          TEXT,
    content      TEXT NOT NULL,
    content_hash TEXT NOT NULL,        -- SHA-256 of chunk text
    document_hash TEXT NOT NULL,       -- SHA-256 of full source file (document-level idempotency)
    embedding    VECTOR(1536),
    content_tsv  TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    ingested_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE (content_hash, version)     -- scoped to version, NOT content_hash alone (see decision D-ING-3)
);
CREATE INDEX ON chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX ON chunks USING GIN (content_tsv);
CREATE INDEX ON chunks (version);

-- ingestion_runs: audit trail per ingestion run
CREATE TABLE ingestion_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source TEXT, version TEXT, branch TEXT,
    files_fetched INT, chunks_produced INT, chunks_inserted INT, chunks_skipped INT,
    duration_ms BIGINT, status TEXT, error_message TEXT,
    run_at TIMESTAMPTZ DEFAULT now()
);

-- eval_runs: CI evaluation history
CREATE TABLE eval_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_at TIMESTAMPTZ DEFAULT now(), git_sha TEXT,
    total INT, passed INT, failed INT, pass_rate NUMERIC(5,2),
    results JSONB
);
```

### 2.3 Ingestion pipeline (`atlas-ingestion`)

```
GitHubDocsFetcher (Contents API + Raw URL, per version/branch)
    → AsciiDocAdapter (AsciidoctorJ .adoc→HTML → Jsoup HTML→text, image alt-text inlined)
    → ChunkingService (jtokkit cl100k_base, sliding window 512 tokens / step 448 / 64 overlap,
                        SHA-256 content_hash + document_hash)
    → EmbeddingService (OpenAI text-embedding-3-small, batch 100, exponential backoff retry)
    → IngestionService orchestrates:
         document_hash unchanged → skip file
         document_hash changed/new → DELETE old chunks for (url, version) → chunk → embed → insert
    → ChunkJdbcWriter (raw JDBC, ON CONFLICT (content_hash, version) DO NOTHING)
    → IngestionRunRepository (writes one ingestion_runs row per version)
```

Config (`atlas.ingestion.*`): chunk-size=512, chunk-overlap=64, batch-size=100,
github.versions = [{1.0-GA, 1.0.x}, {1.1, 1.1.x}, {2.0-M, main}],
include-paths=[api, guides, concepts.adoc, getting-started.adoc, upgrade-notes.adoc],
exclude-files=[contribution-guidelines.adoc, index.adoc].

**Target verified result (matches v1):** 570 chunks / 95 files across 3 versions
(1.0-GA: 29 files/167 chunks, 1.1: 33 files/192 chunks, 2.0-M: 33 files/211 chunks).

### 2.4 Retrieval layer (`atlas-retrieval`)

```
Input: { query, version? }
    ├─► VectorSearchRepository — pgvector cosine, top-20
    └─► BM25SearchRepository  — content_tsv full-text, top-20
              │
        HybridSearchService — embeds question once, runs both, merges via RRF (k=60)
              │
        RerankingService — Cohere rerank-english-v3.0 cross-encoder, top-40 → top-8
              │
        List<ScoredChunk> returned
```

Note: the Cohere reranking step is a v1 addition **beyond** the original ARCHITECTURE.md
design (which specified RRF → top-5 directly, no reranker). Decide explicitly this time
whether to keep it, and if so, write the ADR for it (v1 never did — see Section 5).

### 2.5 Query / API layer (`atlas-api`) — designed in v1, never implemented, build this time

```
POST /api/v1/ask
    QueryController → QueryService.answer(question, version?)
        1. Embed question (same model as ingestion — text-embedding-3-small)
        2. HybridSearchService.search(query, version) → top-8 ScoredChunk
        3. PromptBuilder.build(question, chunks) → Prompt (SystemMessage + UserMessage)
        4. ChatModel.call(prompt) → answer text with [N] citations
        5. CitationParser.parse(answer, chunks) → List<Citation>
        6. Assemble AtlasResponse { answer, citations, availableVersions }

GET  /api/v1/ask/stream   — SSE variant, same pipeline, Flux<String> token stream,
                             final `event: done` carries the full structured AtlasResponse
```

Error contract: 400 (blank question / unknown version), 404 (no chunks for version),
500 (embedding/chat API failure — log server-side, generic message to client, per
global CLAUDE.md rule: never leak stack traces to API responses).

### 2.6 Agent layer (`atlas-agent`)

ReAct loop, 3 tools:
- `searchDocs(query, version)` → delegates to `atlas-retrieval`
- `compileJavaSnippet(code)` → `javax.tools` in-process compile, verifies code examples
- `fetchGithubIssue(issueNumber)` → GitHub REST API, unauthenticated, rate-limit aware

Model split: Haiku for tool-call steps, Sonnet for final answer (cost vs. quality trade-off).
`PromptBuilder` (already drafted in v1, uncommitted) separates SystemMessage (static,
cacheable) from UserMessage (dynamic — chunks + question) specifically so the system
prompt can be prompt-cached later (Section 2.7).

### 2.7 Prompt caching

Cached prefix = system prompt (~350 tokens) + static framework-context block (~800 tokens)
≈ 1,350 tokens — above Anthropic's 1,024-token cache-eligibility minimum.
Metrics: `atlas.cache.hit`/`miss`, `atlas.latency.ms`, `atlas.cost.cents` via Micrometer.

### 2.8 Structured output

```json
{
  "answer": "string",
  "code_block": "string | null",
  "imports": ["string"],
  "maven_dep": "string | null",
  "gradle_dep": "string | null",
  "version_tag": "1.0-GA | 1.1 | 2.0-M",
  "citations": [{ "section": "...", "url": "...", "excerpt": "..." }]
}
```
Enforced via Spring AI `BeanOutputConverter`. Non-conformance → HTTP 422 + log raw output;
track non-conformance rate as a metric (revise prompt if > 2%).

### 2.9 Evals (`atlas-evals`)

50-question dataset (`spring-ai-qa.json`) covering API usage, migration questions,
config questions, code-must-compile questions. Per-question checks: citation present,
code compiles, answer similarity ≥ threshold. **80% pass rate gates the PR** — JUnit 5
test class run via `mvn test -pl atlas-evals` in GitHub Actions; results written to
`eval_runs`; PR comment posts pass rate + failed-question links.

### 2.10 MCP server (`atlas-mcp`)

One tool, `ask_atlas(query, version)`, stdio transport, delegates to `atlas-retrieval`
+ `atlas-agent` as library modules. HTTP/SSE transport deferred to v2.

---

## 3. Build Sequence & Live Progress Tracker

**Update this table at every commit — this is the thing v1 stopped doing after 2026-05-16.**

| # | Module | Exit condition | Status | Commit |
|---|---|---|---|---|
| 1 | `infra` | Flyway migration applies cleanly; `\d chunks` shows correct schema | ⬜ Not started | |
| 2 | `atlas-core` | Domain model compiles; unit tests pass | ⬜ Not started | |
| 3 | `atlas-ingestion` | 1,000+ chunks in DB (target: 570 across 3 versions, matching v1) | ⬜ Not started | |
| 4 | `atlas-retrieval` | Top-5/8 retrieval correct for 10 hand-checked questions | ⬜ Not started | |
| 5 | `atlas-api` (basic) | `POST /api/v1/ask` returns valid structured JSON for 10 test questions | ⬜ Not started | |
| 6 | `atlas-api` (streaming) | Browser/curl shows SSE token stream | ⬜ Not started | |
| 7 | `atlas-agent` | Agent calls `searchDocs` before answering; `compileJavaSnippet` before returning code | ⬜ Not started | |
| 8 | Prompt caching | Cache hit/miss + latency delta logged | ⬜ Not started | |
| 9 | `atlas-evals` | 50-question dataset; CI job visible; <80% blocks merge | ⬜ Not started | |
| 10 | `atlas-mcp` | Claude Desktop calls `ask_atlas`, gets a response | ⬜ Not started | |

Steps 1–6 = working RAG service (the core deliverable). Steps 7–10 = agent/caching/evals/MCP
(the differentiator for interviews — most candidates stop at step 6).

---

## 4. Process Rules (carry forward from v1 — these worked)

From `~/.claude/CLAUDE.md` (global) + v1's own `CLAUDE.md`/`guidelines.md`:

1. **Design before code** — state each class's single responsibility in one sentence before writing it.
2. **SOLID check** every class before implementation (table in guidelines.md).
3. **Compile before every commit** (`mvn compile`).
4. **Commit at logical checkpoints**, not per file. No `Co-authored-by: Claude`.
5. **`.env` for secrets**, `.env.example` kept current, never committed.
6. **Constructor injection only** — no field `@Autowired`. `private final` fields.
7. **Flyway**: never edit an applied migration, always add a new `V{n}__`.
8. **At every commit, update `docs/progress.md` (checklist above) and `docs/decisions.md` (new ADRs).**
   This is the rule that slipped last time — 4 commits landed with no progress.md update.
   Treat an unupdated progress.md as a sign the commit isn't actually finished.
9. **After finishing a module, add a Q&A section to `docs/interview-prep.md`** (v1 only ever
   did this for ingestion — do it for every module this time, while the reasoning is fresh).

---

## 5. Decisions carried over from v1 (condensed ADR log)

| # | Decision | Why |
|---|---|---|
| D1 | Stateless, two-turn conversation flow — client carries `version` between turns | Simpler than server sessions, scales horizontally |
| D2 | Spring AI over LangChain4j | Native Spring Boot integration; LangChain4j deferred to v2 |
| D3 | `DocumentAdapter` interface (Open/Closed) | Adding new source formats later needs zero changes to existing adapters |
| D4 | AsciidoctorJ + Jsoup (two-step) for .adoc extraction | Regex stripping is too fragile; AsciidoctorJ 3.x removed `headerFooter()` — use `standalone(false)` |
| D5 | jtokkit `cl100k_base` for token counting | Same encoding as `text-embedding-3-small` — no off-by-one drift between chunking and the model |
| D6 | Two-level idempotency: `document_hash` (skip/reprocess whole file) + `content_hash` scoped to `(content_hash, version)` | A file unchanged between versions produces the same `content_hash` — a bare `UNIQUE(content_hash)` would silently drop the second version's row. **This bug actually happened in v1**: 1.1 had 100 chunks instead of 192 until the constraint was rescoped. |
| D7 | SOLID stated per-class before coding | Caught a real violation early: `ChunkingService`/`EmbeddingService` were about to be merged |
| D8 | `progress.md`/`decisions.md` updated at every commit, not end-of-session | No hook exists to auto-capture decisions; commits are the reliable trigger |
| **D9 (undocumented in v1 — do properly this time)** | Cohere `rerank-english-v3.0` cross-encoder added after RRF, top-40→top-8 | Never got an ADR in v1. When rebuilding: decide *why* (RRF alone insufficiently precise? evaluated against what?) before adding it back, and write the ADR this time. |

---

## 6. Interview Prep Bank

v1 already produced a strong Q&A set for **ingestion only** — reproduced in full below so it
isn't lost. Do the same exercise for every other module immediately after building it
(Section 4, rule 9) — retrieval, agent, api, evals, mcp all currently have zero interview
prep written down.

### Ingestion (already interview-ready — see full text preserved at the end of this file for convenience, originally `docs/interview-prep.md`)

Topics already covered in depth: chunking strategy (why 512/64, sliding window vs. hard
split, why token-based not char-based, why cl100k_base), embeddings (why small vs. large,
cosine vs. Euclidean, batch size rationale), idempotency (two-level design, the real bug
that happened, self-healing on crash), pgvector/IVFFlat vs HNSW, SOLID walkthrough of every
ingestion class, Spring Boot internals (`EnvironmentPostProcessor`, `@ConfigurationProperties`,
`ApplicationRunner`), retryable vs fatal errors, and system-design extensions (scale to
10k docs, handle deletions, nightly scheduling, add a second source).

### Retrieval, Agent, API, Evals, MCP — **to be written** as each module is rebuilt

Suggested question categories to fill in per module (mirror the ingestion structure):
- Core concept explanation (what/why, in your own words)
- Why this specific algorithm/library over the obvious alternative
- A real bug or wrong-turn you hit while building it, and what you learned
- SOLID walkthrough of the module's classes
- "How would you scale/extend this?" system-design question

---

## 7. What went wrong last time (read before rebuilding, so it doesn't repeat)

- `docs/progress.md` was updated faithfully through step 3, then went stale for the last
  4 commits (query-pipeline design doc, Mermaid fix, full retrieval implementation).
  Nobody caught it because there's no automated check — it relies on discipline alone.
- `docs/decisions.md` never got an entry for the Cohere reranking addition — a real
  architectural decision made outside the documented process.
- Two files (`PromptBuilder.java`, `atlas_search_context.md`) were left uncommitted in
  the working tree — work started but never checkpointed.
- The project went dormant for ~2 months after the retrieval milestone, right before the
  hardest remaining piece (the API/query layer that ties everything together end-to-end).
  That's usually the highest-risk abandonment point — plan to move through step 5 quickly
  once ingestion + retrieval are solid again, rather than lingering.
- Original `~/Sites/atlas` local clone was abandoned without anyone noticing for months —
  if using multiple machines/clones again, `git fetch` and check `ahead/behind` at the
  start of a session, don't trust `git status` on a clone that hasn't fetched recently.

---

## Appendix: full text of v1's `docs/interview-prep.md` (preserved verbatim)

# Interview Prep — Atlas Ingestion Pipeline

Questions you should be ready to answer based on exactly what was built.
Grouped by topic. Answers are grounded in the actual implementation — not theory.

---

## 1. Chunking Strategy

**Q: Why did you choose 512 tokens as chunk size? What happens if it's too large or too small?**

512 tokens (~380 words) is a well-established sweet spot for `text-embedding-3-small`.
Too large → the embedding averages over too many concepts, making it semantically vague.
Too small → individual chunks lack enough context to be useful in retrieval.
512 fits most Spring AI doc sections cleanly without losing structure.

**Q: What is sliding window chunking and why do you use overlap?**

Instead of cutting the document into hard non-overlapping blocks, a sliding window moves
forward by a step smaller than the window size. Each chunk overlaps with its neighbours.
This ensures sentences that fall near a boundary appear in full in at least one chunk —
preventing a concept from being split across two chunks and lost in retrieval.

**Q: Why 64 tokens of overlap? What's the trade-off?**

64 tokens (~12% of 512) is enough to preserve boundary continuity without bloating chunk
count significantly. More overlap → better boundary coverage but more chunks and more
OpenAI API cost. Less overlap → cheaper but risks missing split concepts.

**Q: Why token-based chunking instead of character or sentence splitting?**

The embedding model (`text-embedding-3-small`) has a hard token limit — not a character
limit. Chunking by tokens guarantees no chunk ever exceeds the model's context window.
Sentence splitting can produce very uneven chunk sizes and risks token limit violations.

**Q: Why `cl100k_base` encoding specifically?**

`text-embedding-3-small` uses `cl100k_base` internally. Using the same tokeniser at
chunking time means token counts at ingestion exactly match what the model sees — no
off-by-one drift. This is why `jtokkit` was chosen over a generic character estimator.

**Q: What happens to a very short document — say, only 100 tokens?**

The sliding window produces a single chunk covering the full document. The step size is
448, so one pass covers tokens 0→512. The chunking loop terminates after one window.

---

## 2. Embeddings

**Q: What is a vector embedding and what does it represent?**

A vector embedding is a fixed-length array of floating point numbers that encodes the
semantic meaning of a piece of text. Texts with similar meaning are close together in
vector space (small cosine distance). `text-embedding-3-small` outputs 1536 floats per
chunk — one point in 1536-dimensional space.

**Q: Why `text-embedding-3-small` and not `text-embedding-3-large`?**

`text-embedding-3-small` (1536 dims) gives strong retrieval quality at ~5× lower cost
and latency than `text-embedding-3-large` (3072 dims). For a documentation RAG system
the quality difference is negligible; the cost difference is significant at scale.

**Q: What does each dimension of the 1536-dimensional vector mean?**

Nothing interpretable individually. The dimensions are learned by the model during
training — they collectively encode semantic position in the model's latent space.
You cannot inspect a single dimension and assign it a human-readable meaning.

**Q: How does cosine similarity work? Why use it over Euclidean distance for text?**

Cosine similarity measures the angle between two vectors — it is 1.0 for identical
direction, 0.0 for orthogonal. It ignores magnitude, which matters for text because
longer documents produce larger magnitude vectors without being "more similar".
Euclidean distance conflates magnitude with direction, making it less reliable for
semantic search over variable-length texts.

**Q: Why batch 100 chunks per API call?**

OpenAI's embedding API accepts up to 2048 inputs per call. 100 is a practical batch
size that keeps each API call fast, limits the blast radius of a retry, and stays well
within rate limits. Sending one chunk per call would be ~100× slower for a 10,000 chunk
corpus.

---

## 3. Idempotency Design

**Q: What is idempotency and why does it matter for an ingestion pipeline?**

An idempotent operation produces the same result regardless of how many times it runs.
For ingestion this means: re-running the pipeline on unchanged documents must not
duplicate data, waste API calls, or corrupt the knowledge base. Without idempotency,
every scheduled re-run would double the chunk count and cost.

**Q: Explain your two-level idempotency strategy.**

| Level | Mechanism | Scope |
|---|---|---|
| Document | `document_hash` (SHA-256 of raw `.adoc`) | Entire file |
| Chunk | `ON CONFLICT (content_hash, version) DO NOTHING` | Individual chunk row |

Document-level: if the file hasn't changed since last ingestion, skip it entirely —
no chunking, no embedding calls, no DB writes.

Chunk-level: if the file changed (or is new), re-chunk and re-embed, then insert.
The `ON CONFLICT` clause is a silent DB safety net that prevents a duplicate row if the
same chunk content appears in two different runs for the same version.

**Q: Why is the `ON CONFLICT` target `(content_hash, version)` and not just `content_hash`?**

A file that is identical between versions (e.g. `aimetadata.adoc` unchanged from 1.0-GA
to 1.1) produces chunks with the same `content_hash`. If the constraint were just
`UNIQUE (content_hash)`, the 1.1 row would be silently rejected — leaving version 1.1
incomplete in the DB. Scoping to `(content_hash, version)` allows the same chunk content
to exist once per version, while still preventing duplicate rows within a single version.

**Q: What bug would have occurred with `UNIQUE (content_hash)` alone?**

Queries filtered by `version = '1.1'` would return incomplete results for any document
shared with 1.0-GA. The system would silently serve partial answers for 1.1 — no error,
no warning. In our first run this caused 1.1 to have 100 chunks instead of 192.

**Q: What happens if the pipeline crashes halfway through a document?**

Partially inserted chunks remain in the DB. On the next run, `findDocumentHash` finds no
complete set for that `url + version` (or finds a different hash if the file changed).
The pipeline deletes all existing chunks for that `url + version` via
`deleteByUrlAndVersion`, then reprocesses the file cleanly from scratch.

---

## 4. pgvector / Database

**Q: What is pgvector and how does it store vectors?**

pgvector is a PostgreSQL extension that adds a native `VECTOR(n)` column type and
vector-specific operators (`<=>` for cosine distance, `<->` for L2, `<#>` for inner
product). Vectors are stored as compact binary arrays alongside regular relational data —
no separate vector database needed.

**Q: What is an IVFFlat index? What does `lists = 100` mean?**

IVFFlat (Inverted File with Flat compression) is an approximate nearest-neighbour index.
It clusters the vector space into `lists` Voronoi cells at build time. At query time,
only the nearest `probes` cells are searched rather than the full table.
`lists = 100` means the index partitions the space into 100 clusters. pgvector recommends
`lists = rows / 1000` (min 100) as a starting point.

**Q: When does IVFFlat hurt performance?**

When the table has very few rows. The index is built on existing data — with little data,
clusters are poorly formed and recall drops. pgvector warns about this ("created with
little data"). We saw this warning on first insert because the table was nearly empty.
The fix is to build the index after the bulk load, not before.

**Q: IVFFlat vs HNSW — what's the difference?**

| | IVFFlat | HNSW |
|---|---|---|
| Build time | Fast | Slow |
| Query speed | Fast | Faster |
| Recall | Good | Better |
| Memory | Low | High |
| Supports `INSERT` incrementally | Yes (but degrades) | Yes (stays accurate) |

HNSW is generally preferred for production. IVFFlat is fine for a corpus of ~570 chunks.

**Q: Why JDBC for the insert instead of JPA/Hibernate?**

JPA cannot express `ON CONFLICT (content_hash, version) DO NOTHING` in JPQL. It also
cannot cast a `String` to pgvector's `VECTOR` type using the `?::vector` syntax required
by PostgreSQL. Raw `JdbcTemplate` gives full control over the SQL, the conflict clause,
and the batch size — necessary for correctness and performance.

---

## 5. SOLID & Design

**Q: Walk me through how you applied Single Responsibility Principle.**

Each class has exactly one reason to change:

| Class | Single responsibility |
|---|---|
| `GitHubDocsFetcher` | Fetch raw `.adoc` from GitHub API |
| `AsciiDocAdapter` | Convert `.adoc` markup to plain text |
| `ChunkingService` | Split text into token windows + hash |
| `EmbeddingService` | Call OpenAI embedding API with retry |
| `ChunkJdbcWriter` | Write chunks to DB via JDBC |
| `ChunkRepository` | Query/delete chunks via Spring Data |
| `IngestionService` | Coordinate the pipeline steps |
| `IngestionCli` | Boot Spring and trigger ingestion |

If the embedding model changes, only `EmbeddingService` changes.
If the chunking algorithm changes, only `ChunkingService` changes.

**Q: Why is `ChunkingService` separate from `EmbeddingService`?**

They have different reasons to change and different dependencies.
`ChunkingService` depends on `jtokkit` (tokeniser). `EmbeddingService` depends on
OpenAI's API. Combining them would violate SRP — a tokeniser change would force
re-testing the embedding logic and vice versa. They are also independently testable in
isolation.

**Q: Why does `IngestionService` exist — why not put everything in `IngestionCli`?**

`IngestionCli` is a Spring Boot entry point — its job is to start the application and
hand off. Putting pipeline logic there mixes infrastructure concerns (Spring Boot
lifecycle) with business logic (fetch/chunk/embed/store). `IngestionService` is also
independently testable and reusable — e.g. it could be triggered by a REST endpoint or
a scheduled job without touching `IngestionCli`.

**Q: What is the `DocumentAdapter` interface for?**

Open/Closed Principle. Today we have `AsciiDocAdapter` for `.adoc` files. If we add
a Markdown source or a PDF crawler, we add a new `DocumentAdapter` implementation —
no existing code changes. `IngestionService` depends on the abstraction, not the
concrete class.

**Q: Why constructor injection over field injection?**

Constructor injection makes dependencies explicit and mandatory — the object cannot be
created in an invalid state. Fields are `private final` which enforces immutability.
Field injection (`@Autowired` on a field) hides dependencies, requires reflection, and
makes the class harder to test without a Spring context.

---

## 6. Spring Boot Internals

**Q: What is `EnvironmentPostProcessor` and why did you need it?**

`EnvironmentPostProcessor` is a Spring Boot hook that fires before any beans are created
— even before `@Configuration` classes are processed. We needed it to load `.env` values
into the Spring `Environment` early enough that autoconfiguration (which validates the
OpenAI API key on startup) could see them.

**Q: Why can't you call `Dotenv.load()` in `main()` before `SpringApplication.run()`?**

`SpringApplication.run()` triggers autoconfiguration internally during startup. By the
time your next line in `main()` would run, it's too late — autoconfiguration has already
tried to validate the API key and failed. `EnvironmentPostProcessor` hooks in during
the startup sequence, before autoconfiguration fires.

**Q: What is `@ConfigurationProperties` and why is it better than `@Value`?**

`@ConfigurationProperties` binds a structured block of YAML/properties to a typed Java
object with full validation support. `@Value` binds one property at a time to a field —
it becomes unwieldy with 10+ related properties, doesn't support nested structures
cleanly, and scatters configuration across the class. `@ConfigurationProperties` is
a single place to see all config for a feature.

**Q: What is `ApplicationRunner` and when does it fire?**

`ApplicationRunner` is a Spring Boot callback interface. Its `run()` method fires after
the application context is fully started and all beans are initialised — but before the
process exits. Used in CLI tools to execute logic on startup without building a
persistent server.

---

## 7. Error Handling & Resilience

**Q: What is exponential backoff and why is it right for rate limits?**

Exponential backoff doubles the wait time between retries: 2s → 4s → 8s.
For rate limits (HTTP 429), the server is telling you to slow down. Retrying immediately
just hits the limit again. Exponential backoff gives the rate limit window time to reset
before each retry — it's respectful of the API contract.

**Q: What is the difference between a retryable and a fatal error?**

| Retryable | Fatal |
|---|---|
| HTTP 429 — rate limit (temporary) | HTTP 401 — invalid API key (config error) |
| HTTP 500/503 — server error (transient) | HTTP 400 — bad request (programming error) |
| Network timeout | Missing required configuration |

Retryable errors are temporary — waiting and retrying is likely to succeed.
Fatal errors are permanent — retrying will always fail and the right action is to
log clearly and abort.

**Q: What happens to already-inserted chunks if the pipeline fails mid-run?**

They remain in the DB — partial data for the affected document. On the next run,
`findDocumentHash` either finds no hash for that url+version (if no chunks were inserted)
or finds an incomplete set. In either case, `deleteByUrlAndVersion` cleans up the partial
data before re-inserting. The pipeline is self-healing across runs.

---

## 8. System Design (broader)

**Q: Why crawl GitHub directly instead of using the Spring AI documentation website?**

The GitHub repository is the source of truth — structured, version-controlled, one
`.adoc` file per topic. The website is generated HTML with navigation chrome, ads, and
layout noise that degrades chunk quality. GitHub also provides a stable REST API with
version/branch filtering, which makes multi-version ingestion straightforward.

**Q: How would you handle a document being deleted from GitHub?**

Current design does not handle deletions — it only processes files it fetches. To handle
deletions: after fetching the file list for a version, query the DB for all `url` values
for that version, compute the difference (DB urls not in the fetched list), and delete
those chunks. This would be added to `IngestionService.ingestVersion()`.

**Q: How would you scale this to 10,000 documents?**

- Parallelise document processing with a thread pool (`ExecutorService` or virtual threads)
- Move to a queue-based architecture — each document is a message, workers process in parallel
- Cache GitHub API responses — avoid re-fetching unchanged files
- Use `document_hash` to skip unchanged files aggressively (already implemented)
- Pre-filter with BM25 before embedding to reduce OpenAI API calls

**Q: How would you run this on a nightly schedule?**

Option 1: Spring `@Scheduled` + convert `IngestionCli` to a long-running service.
Option 2: GitHub Actions cron job triggering `mvn exec:java`.
Option 3: Kubernetes CronJob.
`IngestionService` is already self-contained and reentrant — no changes needed to the
pipeline logic itself.

**Q: What would you change to support a second source — e.g. Spring Boot docs?**

Add a new `DocumentAdapter` implementation (e.g. `HtmlDocAdapter` for the Spring Boot
website). Add a new `Fetcher` (e.g. `WebCrawlerFetcher`). Add configuration under
`atlas.ingestion.sources`. `IngestionService` iterates sources — it would call each
fetcher/adapter pair. The chunk/embed/store pipeline is unchanged.

---

## Quick-fire answers to have ready

| Question | One-line answer |
|---|---|
| What is RAG? | Retrieve relevant context from a knowledge base, inject into LLM prompt, generate grounded answer |
| What is pgvector? | PostgreSQL extension for native vector storage and similarity search |
| What is cosine similarity? | Measures angle between vectors — 1.0 = identical direction, ignores magnitude |
| What is IVFFlat? | Approximate NN index — clusters vector space, searches only nearest clusters at query time |
| What is a token? | Smallest unit the LLM operates on — roughly ¾ of a word on average in English |
| Why SHA-256 for hashing? | Collision-resistant, deterministic, fast — ideal for idempotency checks |
| Why not store raw embeddings as JSONB? | JSONB has no vector operators — can't do cosine search natively without pgvector |
| What is Spring AI? | Spring abstraction over LLM providers — unified API for chat, embedding, vector store |
