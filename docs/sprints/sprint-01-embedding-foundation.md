# Sprint 1 — Embedding Foundation

## Goal

Build the foundation of Atlas by creating a multi-module project, integrating Spring AI with
PostgreSQL + PGVector, implementing the complete document ingestion, chunking, embedding
generation, and similarity search workflow.

**Deliverable:** A working application that ingests Spring documentation, splits it into chunks,
generates embeddings, stores them in PostgreSQL + PGVector, and retrieves semantically similar
chunks using vector search.

---

## LLM Fundamentals — Integrated into This Sprint

Learn these before writing any code. Each concept explains a decision you are about to make.

**Tokens and tokenization**
- *What:* The embedding model splits text into tokens — subword units, not words. `text-embedding-3-small` uses the cl100k_base tokenizer. "ChatClient" is one token; "ChatClientBuilder" may be two.
- *Why it matters here:* Chunk size (512 tokens) is measured in tokens, not words or characters. Getting this wrong produces chunks that exceed the model's input limit.
- *Interview:* "What is a token? Why do RAG systems measure chunk size in tokens rather than characters?"

**Embedding models vs generative models**
- *What:* An embedding model converts text into a fixed-size vector — no text output. A generative model produces text. Atlas uses both: `text-embedding-3-small` for indexing, Claude/GPT for answering.
- *Why it matters here:* You are configuring Spring AI's `EmbeddingModel` — not `ChatModel`. Confusing the two is the most common setup mistake.
- *Interview:* "What is the difference between `EmbeddingModel` and `ChatModel` in Spring AI?"

**Embedding dimensions and model selection**
- *What:* `text-embedding-3-small` outputs 1536-dimensional vectors. Dimensions define the size of the semantic space. More dimensions can capture more nuance but cost more storage and compute.
- *Why it matters here:* The `chunks` table schema is fixed at `vector(1536)` for this model. If you switch models, the dimension may change and the schema must be updated.
- *Interview:* "What does 1536 dimensions mean? What happens if you change the embedding model?"

**Semantic representation — why vectors capture meaning**
- *What:* The vector encodes semantic meaning, not keywords. Chunks with similar meaning produce vectors that are close in cosine distance, even if they share no words.
- *Why it matters here:* This is the core reason vector search exists. Understanding it is what lets you explain why cosine similarity works as a retrieval metric.
- *Interview:* "How does vector similarity search find relevant content without keyword matching?"

**Embedding model changes require full re-embedding**
- *What:* Vectors from different models (or different dimensions) live in incompatible spaces. A query embedded with model B cannot be compared against chunks embedded with model A.
- *Why it matters here:* This is an architectural constraint — not a bug. If the embedding model changes, 1,260 existing chunks become stale and must be re-embedded.
- *Interview:* "What happens to your vector store if you switch embedding models?"

**Hosted vs open-source embedding models**
- *What:* `text-embedding-3-small` is a hosted OpenAI API. Alternatives like `sentence-transformers` run locally. Trade-offs: hosted = simple, reliable, data leaves your environment; self-hosted = no per-token cost, full data control, but GPU infrastructure required.
- *Why it matters here:* For Atlas, hosted is the right choice at this stage. For enterprise use, data privacy often requires self-hosted.
- *Interview:* "What are the trade-offs between hosted and self-hosted embedding models?"

---

## Build Tasks

### 1. Project Setup
- Create Maven multi-module project
- Create `atlas-parent`
- Create `atlas-api`
- Create `atlas-engine`
- Create `atlas-common`
- Configure dependency management

### 2. Database Setup
- Install PostgreSQL
- Enable PGVector extension
- Create database
- Design document chunk table
- Verify database connectivity

### 3. Configure Spring AI
- Configure OpenAI API
- Configure `EmbeddingModel`
- Configure `VectorStore`
- Externalise application properties

### 4. Document Ingestion
- Read Spring AI documentation
- Explore Spring AI Document Readers
- Load documents into memory
- Verify document contents

**Learn:**
- What is document ingestion?
- Supported document formats
- Spring AI Document abstraction

### 5. Document Chunking
- Split documents into chunks
- Configure chunk size
- Configure overlap
- Compare different chunking strategies
- Verify generated chunks

**Learn:**
- Why chunking is required
- Chunk size vs retrieval quality
- Chunk overlap
- Spring AI `TextSplitter` / `TokenTextSplitter`

### 6. Generate Embeddings
- Create Embedding Service
- Generate embeddings for each chunk
- Verify vector dimensions
- Handle embedding errors

### 7. Store Embeddings
- Persist chunk embeddings in PGVector
- Store chunk metadata
- Store source document information
- Verify stored vectors

### 8. Build Similarity Search
- Generate embedding for user query
- Perform vector similarity search
- Return Top-K matching chunks
- Display similarity scores

### 9. Build REST API
- Create ingestion endpoint
- Create embedding endpoint
- Create search endpoint
- Test APIs using Postman
- Validate responses

### 10. End-to-End Testing
- Ingest Spring documentation
- Chunk documents
- Generate embeddings
- Store vectors
- Execute similarity search
- Verify expected results

---

## Concepts Learned

| Area | Topics |
|---|---|
| Architecture | Multi-module Maven Project, Spring AI Architecture |
| Ingestion | Document Readers, Document Abstraction, Document Ingestion Pipeline |
| Chunking | Chunking, Chunk Size, Chunk Overlap, Chunking Strategies, Text Splitters |
| Embeddings | Embeddings, Embedding Models, OpenAI Embedding API, Embedding Dimensions |
| Vector Storage | Vector Databases, PGVector, Vector Data Type |
| Retrieval | Similarity Search, Cosine Similarity, Top-K Retrieval, Semantic Search |

---

## Sprint Outcome

```
Spring Documentation
        │
        ▼
Spring AI Document Reader
        │
        ▼
  Document Chunking
        │
        ▼
Spring AI EmbeddingModel
        │
        ▼
   Chunk Embeddings
        │
        ▼
 PostgreSQL + PGVector
        │
        ▼
 Similarity Search API
        │
        ▼
Top-K Semantically Similar Chunks
```

---

## Interview Readiness

After Sprint 1, you should be able to confidently answer:

- What is an embedding?
- Why do RAG systems use embeddings?
- What is the difference between `ChatModel` and `EmbeddingModel`?
- Why use PGVector instead of a normal text column?
- Can PostgreSQL be used as a vector database?
- What information is stored alongside an embedding?
- What happens when you call `EmbeddingModel.embed()`?
- How does similarity search work?
- What is Cosine Similarity?
- Why use a multi-module architecture?
