# Atlas RAG — Full Pipeline Architecture

This diagram is the guide for the entire system. Every component here maps to a
module or class in the codebase.

---

## Query Pipeline

![Query Pipeline](diagrams/query-pipeline.svg)

---

## Ingestion Pipeline

![Ingestion Pipeline](diagrams/ingestion-pipeline.svg)

---

## Module Mapping

![Module Mapping](diagrams/module-mapping.svg)

---

## Phase Build Order

| Phase | What we build | What we learn |
|-------|--------------|---------------|
| **A — Vector only** | Vector search against chunks table | Where cosine similarity fails (exact keywords, class names) |
| **B — BM25 only** | Add tsvector column, full-text search | Where keyword matching fails (semantic similarity) |
| **C — Hybrid** | Combine with RRF | Why neither alone is sufficient |

## Component Status

| Component | Status | Module |
|-----------|--------|--------|
| Document Fetcher | ✅ Built | atlas-ingestion |
| Document Parser | ✅ Built | atlas-ingestion |
| Chunker | ✅ Built | atlas-ingestion |
| Embedding | ✅ Configured | atlas-ingestion |
| Vector Store | ✅ Configured | atlas-ingestion |
| Pipeline Orchestrator | ✅ Built | atlas-ingestion |
| Vector Search | ⬜ Phase A | atlas-retrieval |
| BM25 Search | ⬜ Phase B | atlas-retrieval |
| Fusion (RRF) | ⬜ Phase C | atlas-retrieval |
| Reranker | ⬜ Pending ADR-009 | atlas-retrieval |
| Query Router | ⬜ Planned | atlas-agent |
| LLM Generation | ⬜ Planned | atlas-agent |
| Output Validation | ⬜ Planned | atlas-agent |
| REST API | ⬜ Planned | atlas-api |
| SSE Streaming | ⬜ Planned | atlas-api |
| Evals | ⬜ Planned | atlas-evals |
| MCP Server | ⬜ Planned | atlas-mcp |
