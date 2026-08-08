# Architecture Decision Records

Every non-obvious design decision is recorded here.
Updated at every commit checkpoint — never at end of session only.

These first entries are carried forward from the previous build attempt at this project
(condensed in `docs/PLAN.md` Section 5) — restated here as proper ADRs since they still
hold. New decisions made during this rebuild get appended below them.

---

## ADR-001 — Two-turn conversation flow
**Decision:** Stateless design — client carries state (selected version) between turns.
**Reason:** Simpler than server-side session management, scales horizontally, no session cleanup needed.

---

## ADR-002 — Spring AI over LangChain4j
**Decision:** Spring AI 1.1.5 as primary AI framework. LangChain4j deferred to v2.
**Reason:** Native Spring Boot integration, consistent dependency model.

---

## ADR-003 — DocumentAdapter interface for extensibility
**Decision:** `DocumentAdapter` interface with `supports(String fileExtension)` + `extractText(String, String)`.
**Reason:** Open/Closed principle — adding new source formats later requires zero changes to existing classes.

---

## ADR-004 — AsciidoctorJ + Jsoup for text extraction
**Decision:** AsciidoctorJ converts `.adoc` → HTML, then Jsoup strips HTML → plain text.
**Reason:** Regex-based AsciiDoc stripping is too fragile.
**Note:** AsciidoctorJ 3.x removed `headerFooter()` — use `standalone(false)` instead.

---

## ADR-005 — jtokkit for token counting
**Decision:** jtokkit with `cl100k_base` encoding for token counting in `ChunkingService`.
**Reason:** Same encoding as OpenAI `text-embedding-3-small` — accurate token boundaries.

---

## ADR-006 — Two-level idempotency
**Decision:** Document-level (`document_hash`) + DB-level (`content_hash` scoped to `(content_hash, version)`).
**Reason:** `content_hash` alone is insufficient — a file identical across two versions produces
the same `content_hash`; a bare `UNIQUE(content_hash)` silently drops the second version's row.
**This bug actually happened** in the previous build: version 1.1 ended up with 100 chunks
instead of the correct 192 until the constraint was rescoped. Verify this with a test this time
rather than discovering it by counting rows after the fact.

---

## ADR-007 — SOLID enforcement as a project standard
**Decision:** Every class must have its single responsibility stated in one sentence before implementation.
**Reason:** Catches design mistakes before code is written, not after.

---

## ADR-008 — Commits as the trigger for decisions.md + progress.md updates
**Decision:** `decisions.md` and `progress.md` are updated at every commit checkpoint.
**Reason:** No hook exists to automatically detect decisions from conversation; commits are
the reliable trigger. **Note:** this rule was followed for the first ~10 commits of the previous
attempt, then slipped for the last 4 (a full module's worth of work went undocumented). Treat a
commit that doesn't touch these two files as a sign the commit isn't actually finished.

---

## ADR-010 — Chunks-only schema for v1, add tables incrementally
**Decision:** V1 migration creates only the `chunks` table. `ingestion_runs` and `eval_runs` will be added via new migrations when those modules are built.
**Reason:** Build only what the current step needs — no premature schema.

---

## ADR-011 — DocumentFetcher: list() + fetchContent() separation
**Decision:** `DocumentFetcher` interface splits listing (lightweight metadata) from content download (one file at a time).
**Reason:** Downloading all file contents upfront is wasteful for large document sets. The pipeline processes one document at a time: fetch → parse → chunk → embed → store.

---

## ADR-012 — FetchedDocument carries generic metadata map
**Decision:** `FetchedDocument` uses `Map<String, String> metadata` instead of typed fields like `version`.
**Reason:** Version is Spring AI specific. Other sources (S3, Jira, Confluence) have different metadata. A generic map keeps the interface source-agnostic.

---

## ADR-013 — ConfigurationProperties over @Value for structured config
**Decision:** `GitHubProperties` record with `@ConfigurationProperties` instead of `@Value` annotations.
**Reason:** `@Value` cannot bind YAML lists. `@ConfigurationProperties` handles structured/nested config cleanly.

---

## ADR-014 — UUID primary key for chunks table
**Decision:** `UUID` with `gen_random_uuid()` instead of `BIGSERIAL`.
**Reason:** Decouples ID generation from the database — IDs can be created in application code before insert, simplifying testing and batch operations.
**Trade-off:** UUIDs are 16 bytes vs 8, slightly slower to index. Acceptable for our scale.

---

## ADR-015 — No tsvector column in v1 schema
**Decision:** The `chunks` table starts without a `content_tsv` tsvector column or GIN index. BM25 full-text search will be added in a later migration.
**Reason:** Build vector-only search first (Phase A), observe where it fails, then add BM25 (Phase B), then combine as hybrid search (Phase C). Learning by experiencing the problem — not pre-building the solution.

---

## ADR-016 — DocumentParser interface with byte[] input
**Decision:** `DocumentParser.parse(byte[] rawContent)` instead of `parse(String rawContent)`.
**Reason:** Text-based formats (`.adoc`, `.md`) and binary formats (PDF, Word) must share the same interface (Liskov Substitution). `byte[]` is the universal input — text parsers convert to String internally.

---

## ADR-017 — DocumentParser.supports() for strategy selection
**Decision:** Each `DocumentParser` implementation declares `boolean supports(String filename)`. The caller loops all parsers and picks the matching one (Strategy pattern).
**Reason:** Avoids a factory with if/else chains. Adding a new format = adding a new `@Component` implementation. Spring auto-discovers all implementations via `List<DocumentParser>`.

---

## ADR-018 — Separate atlas_rag database
**Decision:** New `atlas_rag` database in the existing `roms-postgres` container, not reusing the old `atlas` database.
**Reason:** Keeps old project data untouched. Clean separation for the rebuild.

---

## ADR-019 — GitHub API over git clone for document ingestion
**Decision:** Fetch `.adoc` files via GitHub Contents API, not by cloning the Spring AI repository.
**Reason:** In production, ingestion sources are APIs (GitHub, S3, Confluence), not local clones. Cloning entire repos doesn't scale — you only need the document content, not the git history.

---

## ADR-020 — Progressive search strategy: vector-only → BM25 → hybrid
**Decision:** Build search in three phases: (A) vector-only, (B) BM25-only, (C) hybrid with RRF.
**Reason:** Experiencing where vector search fails firsthand makes the case for BM25 concrete. Produces a strong interview story: "I observed X failure, researched alternatives, added BM25, combined with RRF."

---

## ADR-009 — Reranking decision — PENDING, decide properly this time

The previous attempt added a Cohere `rerank-english-v3.0` cross-encoder step after RRF
(top-40 → top-8) without ever writing the ADR for it. Before re-adding it: decide and record
here *why* — was RRF alone evaluated and found insufficiently precise? Against what test
questions? If reranking is added again, this entry should be replaced with the real reasoning
and evidence, not backfilled from memory.
