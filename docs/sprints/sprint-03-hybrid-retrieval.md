# Sprint 3 — Hybrid Retrieval

## Goal

Build a retrieval engine that finds the most relevant documentation using both semantic search
and keyword search.

**Deliverable:** A working Hybrid Retrieval engine that returns the best matching document
chunks for a user query.

---

## Build Tasks

### 1. Build Semantic Search
- Generate embedding for the user query
- Search similar vectors using PGVector
- Return Top-K matching chunks
- Display similarity scores

### 2. Experiment with Vector Search
- Compare Cosine Similarity
- Compare Euclidean Distance
- Compare Inner Product
- Evaluate retrieval quality

### 3. Build Keyword Search
- Implement BM25 search
- Search using keywords
- Return ranked keyword matches
- Compare results with semantic search

### 4. Build Hybrid Search
- Combine Semantic Search and BM25
- Merge search results
- Eliminate duplicate results
- Return a unified ranked list

### 5. Implement Reciprocal Rank Fusion (RRF)
- Merge rankings from both searches
- Rank final results using RRF
- Compare retrieval quality before and after RRF

### 6. Add Metadata Filtering
- Filter by Spring AI version
- Filter by document type
- Filter by source file
- Support configurable filters

### 7. Build Retrieval API
- Create `/retrieve` endpoint
- Accept user question
- Return Top-K chunks
- Include similarity score and metadata

### 8. Evaluate Retrieval Quality
- Test with different questions
- Compare Semantic vs BM25 vs Hybrid
- Tune Top-K value
- Fine-tune retrieval parameters

---

## Concepts Learned

- Semantic Search
- Query Embeddings
- Vector Similarity Search
- Cosine Similarity
- BM25
- Hybrid Search
- Reciprocal Rank Fusion (RRF)
- Metadata Filtering
- Top-K Retrieval
- Retrieval Evaluation

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- What is Semantic Search?
- How does Vector Similarity Search work?
- Why is Cosine Similarity commonly used?
- What is BM25?
- Why combine BM25 with Vector Search?
- What is Hybrid Search?
- What is Reciprocal Rank Fusion (RRF)?
- What is Top-K Retrieval?
- How do you evaluate retrieval quality?
