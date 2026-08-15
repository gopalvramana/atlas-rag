# Atlas RAG — Spring AI Learning Vehicle

A Q&A service over Spring AI documentation, rebuilt from scratch as an interview/portfolio
project — one AI-engineering technique per module, in Java, with the design reasoning
documented at every step.

**Full design and current status:** [`docs/PLAN.md`](docs/PLAN.md) and
[`docs/progress.md`](docs/progress.md).

---

## Architecture

### Query Pipeline

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

### Ingestion Pipeline

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

### Module Mapping

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

### Phase Build Order

| Phase | What we build | What we learn |
|-------|--------------|---------------|
| **A — Vector only** | Vector search against chunks table | Where cosine similarity fails (exact keywords, class names) |
| **B — BM25 only** | Add tsvector column, full-text search | Where keyword matching fails (semantic similarity) |
| **C — Hybrid** | Combine with RRF | Why neither alone is sufficient |

---

## Techniques

| Technique | Module |
|---|---|
| Hybrid retrieval (BM25 + pgvector + RRF) | `atlas-retrieval` |
| Tool calling + ReAct agent loop | `atlas-agent` |
| Prompt caching | `atlas-agent` |
| Structured output | `atlas-agent` |
| Streaming (SSE) | `atlas-api` |
| Evals as a CI gate | `atlas-evals` |
| MCP server | `atlas-mcp` |

## Modules

```
atlas-core        Shared domain model
atlas-ingestion   CLI: fetch Spring AI docs from GitHub -> chunk -> embed -> load
atlas-retrieval   Hybrid search: BM25 + pgvector + RRF
atlas-agent       ReAct agent, tool calling, prompt caching, structured output
atlas-api         Spring Boot app: REST + SSE endpoints
atlas-mcp         MCP server: exposes ask_atlas over stdio
atlas-evals       Eval dataset and CI runner
```

## Status

Rebuild in progress — see [`docs/progress.md`](docs/progress.md) for the live checklist.

## Stack

Java 21, Spring Boot 3.4.5, Spring AI 1.1.5, PostgreSQL + pgvector, Anthropic Claude
(Haiku + Sonnet), OpenAI `text-embedding-3-small`.

## License

Apache 2.0
