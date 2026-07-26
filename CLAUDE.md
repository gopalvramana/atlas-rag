# Atlas RAG — Project Guidelines

Extends `~/.claude/CLAUDE.md`. All universal rules apply here too.

Full engineering guidelines: [`docs/guidelines.md`](docs/guidelines.md)

---

## Session Start — Read These First

Before touching any code at the start of a session:
1. Read [`docs/PLAN.md`](docs/PLAN.md) — the master architecture + rebuild spec
2. Read [`docs/progress.md`](docs/progress.md) — know exactly where we are
3. Read [`docs/decisions.md`](docs/decisions.md) — recover all prior decisions

## Commit Rule — Always Update These

At every commit checkpoint, update in the same commit:
- [`docs/progress.md`](docs/progress.md) — mark completed items, update next steps
- [`docs/decisions.md`](docs/decisions.md) — append any new ADRs made since last commit

After finishing a module, add a Q&A section to [`docs/interview-prep.md`](docs/interview-prep.md)
while the reasoning is still fresh — this is the thing that makes the module explainable
in an interview six months from now.

---

## Why this project exists

Interview/portfolio piece: a RAG system over Spring AI documentation, built module by
module to go deep on one AI-engineering technique at a time (see `docs/PLAN.md` Section 1
for the full pitch). The point is not just working code — it's being able to explain every
design decision under interview questioning. Don't skip the design-before-code step even
when the code seems obvious; the discussion is what becomes the interview answer later.

## Running Locally

```bash
# 1. Copy and fill in credentials
cp .env.example .env

# 2. Start PostgreSQL + pgvector (see docs/PLAN.md Section 2.2 for schema)

# 3. Run ingestion CLI (once built)
mvn exec:java -pl atlas-ingestion

# 4. Run API (once built)
mvn spring-boot:run -pl atlas-api
```
