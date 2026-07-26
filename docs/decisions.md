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

## ADR-009 — Reranking decision — PENDING, decide properly this time

The previous attempt added a Cohere `rerank-english-v3.0` cross-encoder step after RRF
(top-40 → top-8) without ever writing the ADR for it. Before re-adding it: decide and record
here *why* — was RRF alone evaluated and found insufficiently precise? Against what test
questions? If reranking is added again, this entry should be replaced with the real reasoning
and evidence, not backfilled from memory.
