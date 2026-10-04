# AI Engineer / AI Architect Roadmap

This roadmap is calibrated for a **Java/Spring/cloud architect transitioning into AI engineering**.
The goal is not to become an ML researcher. The goal is to be able to say:

> "I'm a Java/Spring/cloud architect who has built a production-oriented RAG and agent platform,
> and I understand the architecture, retrieval, agent orchestration, evaluation, observability,
> security, scalability and enterprise integration aspects of GenAI systems."

---

## Your Existing Foundation

You already have the left side of this picture. We are adding the right side.

```
          YOUR EXISTING STRENGTH
                   │
  Java / Spring / Cloud / Distributed Systems
                   │
                   ▼
          AI APPLICATION LAYER
                   │
      ┌────────────┼────────────┐
      ▼            ▼            ▼
    RAG          Agents       AI APIs
      │            │            │
 Retrieval     Tool Calling  LLM Integration
      │            │            │
      └────────────┼────────────┘
                   ▼
          AI PLATFORM LAYER
                   │
   Models / Vector DB / MCP / Evaluation
                   │
                   ▼
        PRODUCTION AI ARCHITECTURE
                   │
   Security / Cost / Scale / Observability
   Governance / Reliability / Multi-tenancy
```

---

## The Roadmap

| Phase | Sprint | Focus | Priority |
|---|---|---|---|
| Foundation | 1 | Embeddings + Vector DB | High |
| Foundation | 2 | Knowledge Ingestion | Critical |
| RAG | 3 | Hybrid Retrieval | Critical |
| RAG | 4 | RAG Question Answering | Critical |
| Agents | 5 | Agentic AI + Tools | Critical |
| Agents | 6 | Advanced AI Applications | High |
| Production AI | 7 | Evaluation + Observability | Critical |
| Production AI | 8 | MCP + AI Integration | High |
| Architecture | 9 | Enterprise Production AI Architecture | Critical |
| Parallel track | — | Python AI Literacy | High |

---

## Sprint 1 — Embedding Foundation

**Goal:** Understand the fundamental mechanics of semantic retrieval.

```
Text → Embedding Model → Vector (1536 dims) → PGVector → Similarity Search
```

### Learn deeply
- What embeddings are and why they exist
- Embedding models and dimensions
- Semantic similarity vs keyword matching
- Cosine similarity
- PGVector and HNSW index
- Top-K retrieval
- Vector storage trade-offs

### Interview questions you must answer
- Why embeddings instead of keyword search?
- Why a vector database?
- Why cosine similarity?
- What does 1536 dimensions mean?
- What happens if you change the embedding model?
- HNSW vs brute-force nearest-neighbour search?

**Architecture depth: Medium** — don't over-invest in Maven structure here.

---

## Sprint 2 — Knowledge Ingestion

**Goal:** Build the pipeline that creates the knowledge base.

```
Source → Fetcher → Parser → Chunker → Embedding → Vector Store
```

### Learn deeply
- Chunking strategy and why it matters
- Tokenization and token-based chunking
- Chunk overlap and its effect on retrieval
- Metadata — what to store and why
- Duplicate detection and idempotency
- Re-ingestion handling
- Batch embeddings vs individual calls
- API rate limits and failure/retry
- Ingestion scalability

### Principal-level questions
- Why 512 tokens? Why 64 overlap?
- What happens if the document changes?
- How do you avoid embedding the same document twice?
- How would you ingest 10 million documents?
- How would you make ingestion asynchronous?

**This is much more important than explaining why you created four Maven modules.**

---

## Sprint 3 — Hybrid Retrieval

**Goal:** Build a retrieval engine that finds the most relevant results — not just similar ones.

```
         Query
           │
  ┌────────┴────────┐
  ▼                 ▼
Vector Search      BM25
  │                 │
  └────────┬────────┘
           ▼
          RRF
           ▼
         Top-K
```

### Learn deeply
- Semantic search vs lexical (BM25) search
- Why vector search alone is not enough
- Reciprocal Rank Fusion — how and why
- Metadata filtering
- Reranking (cross-encoder vs bi-encoder)
- Recall vs precision trade-offs
- Retrieval latency

### Key architect question
> "Why isn't vector search alone enough?"

This is one of the most important AI Architect questions. Know it cold.

---

## Sprint 4 — RAG Question Answering

**Goal:** Turn retrieval into an AI application that generates grounded answers.

```
Question → Query Understanding → Hybrid Retrieval → Context
    → Prompt → LLM → Answer + Citations
```

### Learn deeply
- Prompt construction — system vs user prompt
- Context selection and ordering
- Context window management
- Hallucination — causes and prevention
- Citations and grounding
- Model selection (Claude / GPT / Bedrock)
- Temperature and output control
- Structured output
- Streaming responses

### Architect questions
- How do you prevent hallucination?
- How much context should you send to the LLM?
- What happens when retrieval returns poor results?
- How do you choose between Claude, GPT, or Bedrock?
- How do you control token cost?

**Your Java + enterprise architecture background becomes a real advantage here.**

---

## Sprint 5 — Agentic AI

**Goal:** Transform Atlas from a RAG pipeline into an AI Agent that reasons and uses tools.

```
            User
              │
            Agent
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
searchDocs  compile   GitHub
    │         │         │
    └─────────┼─────────┘
              ▼
           Agent
              │
          Response
```

### Learn deeply
- Agent vs deterministic workflow — when to use each
- Tool calling / function calling
- ReAct pattern (Reason + Act)
- Planning and tool selection
- Multi-step execution
- Agent state management
- Failure handling and retries
- Tool security and authorization
- Human-in-the-loop approval

### Architect questions
- When should you use an agent vs a deterministic workflow?
- Why not just call tools directly without an agent loop?
- How do you prevent an agent from executing dangerous actions?
- How do you control runaway agent loops?

**High priority** — current AI Architect roles are explicitly asking for agentic architectures.

---

## Sprint 6 — Advanced AI Application Architecture

**Goal:** Make Atlas production-quality with memory, streaming, model routing, and caching.

```
            AI Gateway
                │
    ┌───────────┼───────────┐
    ▼           ▼           ▼
  Claude       GPT       Bedrock
    │           │           │
    └───────────┼───────────┘
                ▼
           AI Service
```

### Learn deeply
- Conversation memory strategies
- Structured outputs and schema validation
- Streaming responses (SSE / WebFlux)
- Model routing and fallback models
- Response caching
- Token optimisation
- Asynchronous AI workflows
- Structured tool contracts

---

## Sprint 7 — Evaluation & Observability

**Goal:** Build the framework to measure whether Atlas is working and improving.

```
User → AI Application → LLM / Retrieval / Tools
                              │
                       Observability
                              │
               ┌──────────────┼──────────────┐
               ▼              ▼              ▼
           latency          tokens         quality
           errors           cost           failures
```

### Learn deeply

**Evaluation**
- Retrieval evaluation (precision, recall, MRR)
- Answer evaluation (relevance, completeness, groundedness)
- Hallucination detection
- Citation correctness
- Evaluation datasets
- Regression testing for AI systems

**Observability**
- Latency at each pipeline stage
- Token usage and cost per request
- Model call tracing
- Retrieval traces
- Tool call logging
- Prompt and version tracking

**This sprint is not optional for an Architect role.**

---

## Sprint 8 — MCP Integration

**Goal:** Expose Atlas as an MCP Server so AI clients can interact with it as a platform.

```
Claude / AI Client
        │
       MCP
        │
        ▼
   Atlas MCP Server
        │
        ▼
   Atlas Agent
        │
   ┌────┼────┐
   ▼    ▼    ▼
 RAG  Tools  APIs
```

### Learn deeply
- MCP Client and MCP Server
- Tools, resources, and prompts in MCP
- Tool discovery
- Transport configuration
- Security in MCP
- Enterprise integration via MCP

### The key architect question
> "Why MCP instead of ordinary REST APIs?"

Know this answer at architectural depth, not just implementation.

---

## Sprint 9 — Enterprise Production AI Architecture

**Goal:** Answer "How would I run Atlas inside a Fortune 500 enterprise?"

This sprint turns "I built a RAG application" into "I can architect an enterprise AI platform."

### 1. Security
- Authentication and authorisation for AI APIs
- Tenant isolation in multi-tenant AI systems
- PII detection and redaction
- Data leakage prevention
- Prompt injection and jailbreak defences
- Tool authorisation and secrets management

### 2. Scalability
- Ingestion at scale (millions of documents)
- Vector DB scaling strategies
- Async ingestion pipelines and queues
- Embedding cache
- Horizontal scaling of AI services

### 3. Reliability
- LLM provider timeouts and outages
- Retry with exponential backoff
- Fallback models
- Circuit breaker pattern for LLM calls
- Degraded mode (retrieval-only fallback)

### 4. Cost Management
- Token economics — prompt + completion costs
- Embedding cost at scale
- Model routing (cheap model for simple queries, expensive for complex)
- Response caching
- Context reduction strategies
- Batch processing for async workloads

### 5. Enterprise Architecture
```
API Gateway → AI Gateway → Model Abstraction Layer
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
         RAG Service       Agent Service     Tool Service
              │                 │                 │
              └─────────────────┼─────────────────┘
                                ▼
              Vector DB / Enterprise Data / Event Bus
                                │
                          Observability
```

### 6. Governance
- Model approval and change management
- Prompt versioning and rollback
- Audit logging for AI decisions
- Data lineage for RAG sources
- Evaluation gates before promotion
- Responsible AI policies

---

## Parallel Track — Python AI Literacy

Run this alongside the sprints. You need **enough Python to work in AI teams**, not ML research depth.

### You need
- Read and write basic Python
- FastAPI for building AI microservices
- Call LLM APIs (OpenAI, Anthropic SDK)
- Manipulate JSON responses
- Understand LangChain / LangGraph examples
- Work with Jupyter notebooks
- Understand basic ML terminology

### You do not need
- Advanced NumPy / Pandas
- PyTorch or TensorFlow internals
- Model training or fine-tuning
- Deep learning mathematics
- ML research papers

Unless you later decide to target ML Engineer or Applied Scientist roles.

---

## The Positioning Statement

After completing this roadmap, you should be able to say:

> "I'm a Java/Spring/cloud architect who has designed and built a production-oriented RAG and
> agent platform end-to-end. I understand embedding pipelines, hybrid retrieval, agent
> orchestration, prompt engineering, evaluation, observability, security, and enterprise
> integration for GenAI systems — and I can bridge the AI application layer with the
> enterprise infrastructure and Java backend systems that organisations already run."

That is a credible, differentiated position in the current market. It does not require
competing with ML researchers. It requires combining what you already know with what
Atlas teaches you.
