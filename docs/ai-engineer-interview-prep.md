# AI Engineer / AI Architect Interview Prep

This page covers the AI/RAG engineering questions you should be able to answer deeply
after building Atlas. These are the questions that dominate AI engineering interviews
at senior and principal level — not just architecture questions, but hands-on AI system
design questions grounded in real implementation decisions.

---

## 1. Ingestion

- Why do we need chunking?
- How did you choose 512 tokens / 64 token overlap?
- What happens if chunks are too large? Too small?
- Why token-based chunking instead of character or sentence-based?
- How do you handle duplicate documents on re-ingestion?
- How would you scale ingestion to millions of documents?
- Batch vs individual embedding API calls — when do you choose each?

---

## 2. Embeddings

- What is an embedding?
- Why does `text-embedding-3-small` produce 1536 dimensions?
- What determines embedding dimensions?
- What happens if you change embedding models mid-project?
- Why can't you compare vectors from incompatible models or dimensions?
- What are the cost and latency considerations of embedding API calls?

---

## 3. Vector Retrieval

- Why vector search over traditional keyword search?
- Cosine similarity vs Euclidean distance vs Inner Product — when do you use each?
- What does the similarity score actually mean? What is a "good" score?
- Why IVFFlat / HNSW indexes?
- Approximate vs exact nearest-neighbour search — what is the trade-off?
- How do you choose Top-K?
- Recall vs latency trade-offs in vector search.

---

## 4. RAG

- Why hybrid retrieval instead of vector search alone?
- BM25 vs semantic search — what does each do well?
- Why Reciprocal Rank Fusion (RRF)? How does it work?
- How do you evaluate retrieval quality?
- How do you prevent hallucination in RAG systems?
- How do citations work — how do you tie a claim back to a source chunk?
- Context window management — how do you fit retrieved chunks into the prompt?

---

## 5. Agentic AI

- What is the difference between a RAG pipeline and an AI Agent?
- What is Tool Calling / Function Calling?
- What is the ReAct pattern (Reason + Act)?
- How does an agent decide which tool to invoke?
- How do you handle multi-step tool execution?
- What is conversation memory and how do you implement it?
- What are guardrails and why do you need them?
- How do you handle tool failures or unexpected tool outputs?

---

## 6. Production AI Architecture

- How do you reduce latency in a RAG system?
- How do you manage token cost at scale?
- How do you choose between models (cost vs quality vs latency)?
- How do you implement retries and timeouts for LLM API calls?
- What does observability look like for an AI system?
- How do you evaluate an AI system in production?
- How do you manage prompt versions?
- How do you handle PII in documents and user queries?
- How do you secure an AI API?
- How do you scale a RAG system?
- What happens when your LLM provider goes down?
- How do you detect and handle RAG quality degradation over time?

---

## Note on Architecture Questions

Multi-module design, dependency boundaries, module isolation, and independent testing
are also valid interview topics — especially for a senior/principal Java engineer
building an AI system. But they are secondary to the AI-specific questions above.
Be ready to answer both, with the AI questions answered at depth.
