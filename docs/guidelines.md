# Atlas RAG — Engineering Guidelines

These guidelines apply to every module, every class, and every commit in this project.
They are non-negotiable and must be verified before writing any code.

---

## 1. Design Before Code

- Discuss and agree on the design **before** creating any file or class.
- State the **single responsibility** of every class in one sentence before writing it.
- If the responsibility cannot be stated in one sentence, the design is wrong — split it.

---

## 2. SOLID Principles

Every class must be checked against all five principles before implementation:

| Principle | Check |
|---|---|
| **S** — Single Responsibility | Does this class have exactly one reason to change? |
| **O** — Open / Closed | Can behaviour be extended without modifying existing classes? |
| **L** — Liskov Substitution | Can any implementation of an interface be swapped without breaking the caller? |
| **I** — Interface Segregation | Are interfaces narrow and focused — not bloated with methods callers don't need? |
| **D** — Dependency Inversion | Do classes depend on abstractions (interfaces), not concrete implementations? |

---

## 3. Compile Before Commit

- Run `mvn compile` and verify it passes **before every commit**.
- Never commit code that does not compile.

---

## 4. Commit Discipline

- Commit only at **logical checkpoints** — not after every file.
- A logical checkpoint = a coherent, self-contained piece of work.
- Do **not** add `Co-authored-by: Claude` to commit messages.
- At every commit, update `docs/progress.md` and `docs/decisions.md` (see ADR-008).

---

## 5. Credentials and Configuration

- All secrets (`GITHUB_TOKEN`, `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `COHERE_API_KEY`, `DB_PASSWORD`) go in `.env`.
- `.env` is loaded automatically via **dotenv-java** at startup — no `export` statements needed.
- `.env` is in `.gitignore` — never commit it.
- `.env.example` is the committed reference — keep it up to date.

---

## 6. Infrastructure

- PostgreSQL + pgvector runs in Docker (`roms-postgres` container, or your own).
- Schema versioned with **Flyway** — never modify existing migration files, always add a new `V{n}__description.sql`.

---

## 7. Technology Choices

| Concern | Choice | Notes |
|---|---|---|
| AI framework | Spring AI 1.1.5 | Primary framework |
| Embeddings | OpenAI `text-embedding-3-small` | 1536 dimensions |
| LLM | Anthropic Claude (Haiku for tool steps, Sonnet for final answer) | |
| Token counting | jtokkit (`cl100k_base`) | Same encoding as text-embedding-3-small |
| AsciiDoc parsing | AsciidoctorJ 3.0.1 + Jsoup 1.22.2 | .adoc → HTML → plain text |
| LangChain4j | Deferred to v2 | Not in scope for v1 |

---

## 8. Idempotency Contract

- **Document level:** `document_hash` (SHA-256 of full `.adoc` content) — unchanged → skip entire file; changed → DELETE old chunks + reprocess.
- **Chunk level:** `ON CONFLICT (content_hash, version) DO NOTHING` — scoped to version, not content_hash alone (see ADR-006).

---

## 9. Module Responsibilities

| Module | Responsibility |
|---|---|
| `atlas-core` | Shared domain model — `Chunk`, `Version`, `ChunkSource`, `AtlasQuery`, `Citation`, `AtlasResponse` |
| `atlas-ingestion` | CLI pipeline — fetch → extract → chunk → embed → store |
| `atlas-retrieval` | Hybrid search — BM25 + pgvector + RRF |
| `atlas-agent` | ReAct agent loop with tool calling |
| `atlas-api` | REST + SSE endpoints |
| `atlas-mcp` | stdio MCP server |
| `atlas-evals` | Evaluation suite — evals as CI gate |
