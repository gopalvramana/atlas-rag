# Sprint 2 — Knowledge Ingestion

## Goal

Build a knowledge ingestion pipeline that converts Spring AI documentation into searchable
vector embeddings.

**Deliverable:** An ingestion pipeline that reads documentation, chunks content, generates
embeddings, and stores them in PGVector.

---

## LLM Fundamentals — Integrated into This Sprint

Learn these before writing any code. Each concept explains a decision you are about to make.

**Token-based chunk sizing and overlap**
- *What:* Chunk size (512 tokens) and overlap (64 tokens) are measured in tokens because the embedding model's input limit and LLM context window are both denominated in tokens. A "512-word" chunk is meaningless — a word is 1–4 tokens.
- *Why it matters here:* The `SlidingWindowChunker` uses jtokkit to count tokens precisely. Getting the unit wrong produces chunks that silently exceed the embedding model's input limit (8,191 tokens for `text-embedding-3-small`).
- *Interview:* "Why 512 tokens and 64 overlap? What is the embedding model's token input limit and what happens when you exceed it?"

**Token counting and its role in cost and budgeting**
- *What:* Every embedding API call is billed per token. Every chunk stored is a fixed number of tokens that will later be placed into an LLM context window. Tracking token counts at ingestion time gives you cost visibility and enables context budget planning downstream.
- *Why it matters here:* `jtokkit` (cl100k_base tokenizer) is already in the project. Token counts should be stored in the `chunks` table alongside the text — they are needed by `ContextSelector` in Sprint 3.
- *Interview:* "How do you control embedding API cost at ingestion scale? How does token counting at ingestion relate to the LLM context budget in RAG?"

**Embedding API cost, latency, batching, and caching**
- *What:* Each embedding call has fixed overhead (network round trip, API auth). Batching multiple chunks into one call amortizes this overhead. At scale (millions of chunks), batching is the difference between minutes and hours of ingestion time.
- *Why it matters here:* The ingestion pipeline should batch chunks rather than embed one at a time. Spring AI's `EmbeddingModel.embedForResponse(List<String>)` supports batch calls.
- *Interview:* "How would you scale the ingestion pipeline to 10 million documents? Why does batching matter?"

**Context limits and their practical implications for ingestion**
- *What:* The embedding model has a hard token input limit. The LLM has a context window limit. Both constrain design decisions made during ingestion: chunk size must fit the embedding limit; chunk content should be dense enough to be useful within the LLM's available context budget.
- *Why it matters here:* A chunk that is too large fails at embedding time. A chunk that is too small produces too many low-information chunks that waste the LLM's context window.
- *Interview:* "How does the embedding model's token limit affect chunking strategy? How do chunking decisions made at ingestion affect RAG quality at query time?"

---

## Build Tasks

### 1. Create Ingestion Module
- Create `atlas-ingestion`
- Configure module dependencies
- Integrate with `atlas-engine`

### 2. Read Documentation
- Read Markdown files
- Read text files
- Parse document contents
- Preserve document metadata

### 3. Chunk Documents
- Split large documents
- Configure chunk size
- Configure chunk overlap
- Assign chunk identifiers

### 4. Generate Embeddings
- Generate embeddings for each chunk
- Batch embedding requests
- Handle embedding failures
- Validate generated vectors

### 5. Store Knowledge Base
- Store chunk text
- Store embeddings
- Store metadata
- Verify stored records

### 6. Build Ingestion Pipeline
- Process documents sequentially
- Support batch ingestion
- Handle duplicate documents
- Support re-ingestion

### 7. Build Ingestion Runner
- Execute ingestion from command line
- Monitor ingestion progress
- Display ingestion statistics
- Log processing errors

### 8. Validate Knowledge Base
- Verify stored chunks
- Verify metadata
- Verify embeddings
- Test retrieval using sample queries

---

## Concepts Learned

- Knowledge Ingestion
- Document Parsing
- Chunking
- Chunk Overlap
- Metadata
- Batch Embeddings
- Idempotent Processing
- Knowledge Base Construction

---

## Sprint Outcome

```
Spring AI Documentation
         │
         ▼
   Document Reader
         │
         ▼
      Chunking
         │
         ▼
Spring AI EmbeddingModel
         │
         ▼
 PostgreSQL + PGVector
         │
         ▼
 Searchable Knowledge Base
```

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- Why is chunking required?
- How do you choose chunk size?
- Why use chunk overlap?
- What metadata should be stored?
- How do you avoid duplicate ingestion?
- Why batch embedding requests?
- How is a vector knowledge base created?
