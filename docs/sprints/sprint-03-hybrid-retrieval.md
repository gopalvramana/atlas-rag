# Sprint 3 — Hybrid Retrieval

## Goal

Build the complete retrieval pipeline — from a user query to a final, trimmed context block
ready to pass to an LLM.

Vector search alone is not enough. Class names, method signatures, and exact API identifiers
are retrieved far better by keyword search. This sprint combines both approaches and then
refines the result through reranking and token-aware context selection.

**Deliverable:** A `RetrievalPipeline` that accepts a query and returns a `RetrievedContext`
object — a ranked, deduplicated, token-budget-respecting list of chunks ready for Sprint 4
(RAG Question Answering).

```
Query
  ├── Semantic Search (vector / cosine similarity)
  └── BM25 Search    (full-text / PostgreSQL tsvector)
              ↓
         Merge via RRF
              ↓
           Rerank
              ↓
  Context Selection + Token-Aware Trimming
              ↓
     RetrievedContext  →  Sprint 4
```

---

## Why Each Step Exists

| Step | Why it's here |
|---|---|
| Vector search | Captures semantic similarity — "how does Spring AI handle embeddings?" finds relevant chunks even if none of the words appear verbatim |
| BM25 | Captures exact matches — `ChatClient`, `VectorStore`, `EmbeddingModel` are retrieved precisely by keyword |
| RRF | Merges two ranked lists fairly without needing to normalise or compare their scores directly |
| Rerank | Scores the merged candidates by how well each chunk actually answers the query — more expensive but more precise |
| Context selection | Enforces a token budget; picks the highest-ranked chunks that fit within the LLM's context window |

---

## Build Steps

### Step 1 — Flyway V2 migration: add tsvector column

Add a `search_vector` column to `chunks` and a GIN index for fast full-text search.
Also add a trigger to keep `search_vector` updated automatically on insert/update.

```sql
ALTER TABLE chunks ADD COLUMN search_vector tsvector;
CREATE INDEX idx_chunks_search_vector ON chunks USING GIN(search_vector);
UPDATE chunks SET search_vector = to_tsvector('english', content);
```

### Step 2 — BM25SearchService

PostgreSQL full-text search using `to_tsquery` and `ts_rank`.

```java
// SELECT id, content, ..., ts_rank(search_vector, query) AS score
// FROM chunks, to_tsquery('english', ?) query
// WHERE search_vector @@ query
// ORDER BY score DESC LIMIT ?
```

### Step 3 — HybridSearchService (RRF)

Run both searches in parallel. Fuse the two ranked lists using Reciprocal Rank Fusion:

```
RRF score(chunk) = 1/(k + rank_vector) + 1/(k + rank_bm25)
where k = 60  (standard constant)
```

Chunks that appear in both lists get a higher combined score.
Chunks that appear in only one list still get credit for that rank.

### Step 4 — Reranker

Score the fused Top-N candidates by relevance to the original query.
Start with a simple cross-score reranker (score = vector_score × 0.7 + bm25_score × 0.3).
This can be replaced with a Cohere cross-encoder in a later sprint.

### Step 5 — ContextSelector (token-aware trimming)

Given a token budget (e.g. 6,000 tokens for the context window), greedily pick the
highest-ranked chunks until the budget is exhausted. Use jtokkit (already in the project)
to count tokens per chunk.

Output: `RetrievedContext` — an ordered list of chunks with their source, version, score,
and total token count.

### Step 6 — End-to-end comparison

Run 5 test queries. For each, compare:
- Vector-only results (top 5)
- BM25-only results (top 5)
- Hybrid + reranked results (top 5)

Document where vector wins, where BM25 wins, and where hybrid beats both.
This comparison is an interview question in itself.

---

## Classes to Build

| Class | Location | Responsibility |
|---|---|---|
| `BM25SearchService` | `atlas-retrieval` | PostgreSQL full-text search |
| `HybridSearchService` | `atlas-retrieval` | RRF fusion of vector + BM25 |
| `Reranker` | `atlas-retrieval` | Score and re-order fused candidates |
| `ContextSelector` | `atlas-retrieval` | Token-aware trimming to fit LLM budget |
| `RetrievedContext` | `atlas-retrieval` | Output value object passed to Sprint 4 |
| `V2__add_search_vector.sql` | `atlas-ingestion` resources | Flyway migration |

---

## Concepts Learned

- Why vector search alone is not enough (the most important retrieval question)
- BM25 and how PostgreSQL implements it with `tsvector` / `ts_rank`
- Reciprocal Rank Fusion — how it works and why k=60
- Reranking — bi-encoder vs cross-encoder, cost vs quality
- Token budgets — why you cannot just dump all retrieved chunks into the prompt
- Context window management
- Retrieval precision vs recall trade-offs

---

## Interview Readiness

After this sprint you must be able to answer these at depth:

**The key question — know this cold:**
> "Why isn't vector search alone enough?"

Vector search finds semantically similar chunks. But it misses exact matches.
If a user asks "how do I use `ChatClient.builder()`", vector search may return
chunks about the ChatClient concept. BM25 finds chunks that literally contain
`ChatClient.builder`. Hybrid gives you both.

**Other questions:**
- What is BM25? How is it different from cosine similarity?
- What is Reciprocal Rank Fusion? Why k=60?
- What is a reranker? When would you use a cross-encoder instead?
- What is a token budget? Why can't you send all retrieved chunks to the LLM?
- How do you evaluate retrieval quality?
- What is the difference between recall and precision in retrieval?
- How would you tune Top-K?
