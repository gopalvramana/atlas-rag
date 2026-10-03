# Sprint 1 — Embedding Foundation

## Goal

Build the foundation of Atlas by creating a multi-module project, integrating Spring AI with
PostgreSQL + PGVector, implementing the complete document ingestion, chunking, embedding
generation, and similarity search workflow.

**Deliverable:** A working application that ingests Spring documentation, splits it into chunks,
generates embeddings, stores them in PostgreSQL + PGVector, and retrieves semantically similar
chunks using vector search.

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
