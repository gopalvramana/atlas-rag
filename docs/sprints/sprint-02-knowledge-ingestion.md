# Sprint 2 — Knowledge Ingestion

## Goal

Build a knowledge ingestion pipeline that converts Spring AI documentation into searchable
vector embeddings.

**Deliverable:** An ingestion pipeline that reads documentation, chunks content, generates
embeddings, and stores them in PGVector.

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
