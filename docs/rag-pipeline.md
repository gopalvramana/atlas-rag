# Atlas RAG — Full Pipeline Architecture

This diagram is the guide for the entire system. Every component here maps to a
module or class in the codebase.

```mermaid
%%{init: {
  'theme': 'base',
  'themeVariables': {
    'primaryColor': '#e8eaf6',
    'primaryTextColor': '#1a1a2e',
    'primaryBorderColor': '#7c7cba',
    'lineColor': '#5046e5',
    'secondaryColor': '#e3f2fd',
    'tertiaryColor': '#f3e5f5',
    'background': '#ffffff',
    'mainBkg': '#e8eaf6',
    'nodeBorder': '#7c7cba',
    'clusterBkg': '#f5f5ff',
    'clusterBorder': '#b0b0d0',
    'titleColor': '#1a1a2e',
    'edgeLabelBackground': '#ffffff',
    'fontSize': '18px',
    'nodeSpacing': 30,
    'rankSpacing': 40
  }
}}%%

flowchart TD
    Q[/"🔍 User Query"/]
    Q --> QR

    subgraph QUERY_PROC["🧠 Query Processing"]
        QR["Query Router\nroute by intent"]
        QT["Query Transformation\nrewrite · expand · decompose"]
        QR --> QT
    end

    QT --> VS & BM

    subgraph RETRIEVAL["🔎 Dual Retrieval"]
        direction LR
        VS["Vector Search\npgvector · cosine"]
        BM["BM25 Search\ntsvector · full-text"]
    end

    VS & BM --> FU

    subgraph MERGE["⚙️ Result Merging & Refinement"]
        FU["Fusion\nReciprocal Rank Fusion"]
        RR["Reranker\nCohere cross-encoder"]
        MF["Metadata Filter\nversion · source · type"]
        PC["Parent/Child Context\nexpand to surrounding chunks"]
        CC["Context Compression\nremove redundancy"]
        FU --> RR --> MF --> PC --> CC
    end

    CC --> LLM

    subgraph GEN["🤖 LLM Generation"]
        LLM["Claude\nHaiku for tools · Sonnet for answer"]
    end

    LLM --> CV & GD & CF

    subgraph VALID["✔️ Output Validation"]
        direction LR
        CV["Citation\nValidation"]
        GD["Guardrail\nCheck"]
        CF["Confidence\nScore"]
    end

    CV & GD & CF --> ANS[/"✅ Answer"/]

    ANS --> EV & OB

    subgraph POST["📊 Post-Processing"]
        direction LR
        EV["Evaluation\nevals as CI gate"]
        OB["Observability\nlatency · tokens · scores"]
    end

    subgraph INGEST["📥 Document Ingestion Pipeline"]
        direction LR
        SRC["Source\nGitHub API"]
        PARSE["Parse\nAsciidoctorJ + Jsoup"]
        CHUNK["Chunk\n512-token window"]
        EMBED["Embed\ntext-embedding-3-small"]
        STORE[("PostgreSQL\n+ pgvector")]
        SRC --> PARSE --> CHUNK --> EMBED --> STORE
    end

    STORE -.->|"serves"| VS
    STORE -.->|"serves"| BM

    subgraph MODULES["📦 Module Mapping"]
        direction LR
        M1["atlas-core\ndomain model"]
        M2["atlas-ingestion\nfetch · parse · chunk · embed"]
        M3["atlas-retrieval\nvector + BM25 + RRF"]
        M4["atlas-agent\nReAct · tools · prompts"]
        M5["atlas-api\nREST + SSE"]
        M6["atlas-evals\nCI gate"]
        M7["atlas-mcp\nstdio server"]
    end

    style Q fill:#dcedc8,stroke:#7cb342,color:#1a1a2e
    style ANS fill:#dcedc8,stroke:#7cb342,color:#1a1a2e
    style LLM fill:#e3f2fd,stroke:#1976d2,color:#1a1a2e
    style VS fill:#e8eaf6,stroke:#5046e5,color:#1a1a2e
    style BM fill:#e8eaf6,stroke:#5046e5,color:#1a1a2e
    style FU fill:#f3e5f5,stroke:#8e24aa,color:#1a1a2e
    style RR fill:#f3e5f5,stroke:#8e24aa,color:#1a1a2e
    style STORE fill:#fff3e0,stroke:#f57c00,color:#1a1a2e
    style SRC fill:#dcedc8,stroke:#7cb342,color:#1a1a2e
    style PARSE fill:#dcedc8,stroke:#7cb342,color:#1a1a2e
    style CHUNK fill:#fff9c4,stroke:#f9a825,color:#1a1a2e
    style EMBED fill:#dcedc8,stroke:#7cb342,color:#1a1a2e
```

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
