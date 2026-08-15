# Atlas RAG — Full Pipeline Architecture

This diagram is the guide for the entire system. Every component here maps to a
module or class in the codebase.

---

## Query Pipeline

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {
  'primaryColor': '#e8eaf6', 'primaryTextColor': '#1a1a2e',
  'primaryBorderColor': '#7c7cba', 'lineColor': '#5046e5',
  'clusterBkg': '#f5f5ff', 'clusterBorder': '#b0b0d0',
  'edgeLabelBackground': '#ffffff', 'fontSize': '16px'
}}}%%

flowchart TD
    Q([" 🔍 User Query "]):::entry --> QR
    QR["Query Router"] --> QT["Query Transformation"]
    QT --> VS["Vector Search\npgvector · cosine"]
    QT --> BM["BM25 Search\ntsvector · full-text"]
    VS --> FU["Fusion — RRF"]
    BM --> FU
    FU --> RR["Reranker\nCohere cross-encoder"]
    RR --> MF["Metadata Filter"]
    MF --> PC["Parent/Child Context"]
    PC --> CC["Context Compression"]
    CC --> LLM(["🤖 Claude LLM\nHaiku for tools · Sonnet for answer"]):::llm
    LLM --> CV["Citation Validation"]
    LLM --> GD["Guardrail Check"]
    LLM --> CF["Confidence Score"]
    CV --> ANS([" ✅ Answer "]):::entry
    GD --> ANS
    CF --> ANS
    ANS --> EV["Evaluation"]
    ANS --> OB["Observability"]

    classDef entry fill:#dcedc8,stroke:#7cb342,color:#1a1a2e,stroke-width:2px
    classDef llm fill:#e3f2fd,stroke:#1976d2,color:#1a1a2e,stroke-width:2px
```

---

## Ingestion Pipeline

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {
  'primaryColor': '#e8eaf6', 'primaryTextColor': '#1a1a2e',
  'primaryBorderColor': '#7c7cba', 'lineColor': '#43a047',
  'edgeLabelBackground': '#ffffff', 'fontSize': '16px'
}}}%%

flowchart LR
    SRC["📥 Source\nGitHub API"]:::done --> PARSE["📄 Parse\nAsciidoctorJ + Jsoup"]:::done
    PARSE --> CHUNK["✂️ Chunk\n512-token sliding window"]:::next
    CHUNK --> EMBED["🧮 Embed\ntext-embedding-3-small"]:::done
    EMBED --> STORE[("🗄️ PostgreSQL\n+ pgvector")]:::store

    classDef done fill:#dcedc8,stroke:#7cb342,color:#1a1a2e,stroke-width:2px
    classDef next fill:#fff9c4,stroke:#f9a825,color:#1a1a2e,stroke-width:2px
    classDef store fill:#fff3e0,stroke:#f57c00,color:#1a1a2e,stroke-width:2px
```

---

## Module Mapping

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {
  'primaryColor': '#e8eaf6', 'primaryTextColor': '#1a1a2e',
  'primaryBorderColor': '#7c7cba', 'lineColor': '#5046e5',
  'edgeLabelBackground': '#ffffff', 'fontSize': '16px'
}}}%%

flowchart LR
    CORE["atlas-core\n📦 domain model"] --> ING["atlas-ingestion\n📥 fetch · parse · chunk · embed"]
    CORE --> RET["atlas-retrieval\n🔎 vector + BM25 + RRF"]
    CORE --> AGENT["atlas-agent\n🤖 ReAct · tools · prompts"]
    AGENT --> API["atlas-api\n🌐 REST + SSE"]
    RET --> AGENT
    API --> MCP["atlas-mcp\n🔌 stdio server"]
    CORE --> EVALS["atlas-evals\n📊 CI gate"]
```

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
| Chunker | ⬜ Next | atlas-ingestion |
| Embedding | ✅ Configured | atlas-ingestion |
| Vector Store | ✅ Configured | atlas-ingestion |
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
