# Sprint 6 — Advanced Agent Features

## Goal

Enhance the AI Agent with advanced capabilities such as memory, structured outputs, streaming,
and prompt optimisation to make it more intelligent, reliable, and user-friendly.

**Deliverable:** A conversational AI Agent that supports memory, streaming responses, and
structured outputs.

---

## LLM Fundamentals — Integrated into This Sprint

Learn these before writing any code. Each concept explains a decision you are about to make.

**Model routing based on capability, latency, and cost**
- *What:* Route each request to the cheapest model that reliably handles it. Simple factual RAG lookups → Haiku / GPT-4o-mini. Multi-step reasoning / complex tool orchestration → Sonnet / GPT-4o. Classification-based routing inspects the query before sending it to the model.
- *Why it matters here:* Sprint 6 adds model routing as a feature. Understanding the capability/cost matrix of available models is prerequisite to making routing decisions that are defensible in production.
- *Interview:* "How would you implement model routing? What signals would you use to decide which model handles a request?"

**Small vs large models**
- *What:* Larger models have higher capability on complex tasks, slower inference, and higher cost per token. Smaller models are faster and cheaper but may fail at multi-step reasoning, nuanced instruction following, or reliable structured output. The right model is the smallest one that meets quality requirements for the task class.
- *Why it matters here:* This sprint introduces structured output validation. If the model consistently fails to produce valid JSON, the model may be too small for the task — before tuning the schema, verify the model's structured-output reliability.
- *Interview:* "When would you use a smaller model in a RAG system? What are the failure modes of smaller models?"

**Hosted vs self-hosted models**
- *What:* Hosted APIs (OpenAI, Anthropic, Google) = no GPU infrastructure, simple SDK integration, but data leaves your environment, subject to provider pricing and uptime. Self-hosted (Llama 3, Mistral) = full data control, no per-token cost at inference time, but GPU provisioning, model serving, version management, and performance tuning become your responsibility.
- *Why it matters here:* Sprint 6 connects Atlas to a model gateway. The gateway abstracts the provider. Understanding the trade-offs is necessary to design a gateway that is worth abstracting over.
- *Interview:* "An enterprise customer requires that no data leaves their VPC. How does this affect your model selection and deployment architecture?"

**Quality, latency, cost, and operational complexity trade-offs**
- *What:* These four dimensions always trade off. Higher quality = larger model = higher cost and latency. Lower cost = smaller model = lower quality. Self-hosting = lower per-token cost at scale but adds significant operational complexity. There is no configuration that optimises all four simultaneously.
- *Why it matters here:* Every model routing decision and every LLM gateway design choice involves this trade-off. Being able to articulate it is a principal-level interview expectation.
- *Interview:* "How would you present the model selection decision to an engineering leadership audience?"

**Token optimization and context reduction**
- *What:* Reduce prompt size by: (1) trimming low-relevance chunks; (2) summarising conversation history rather than passing all turns; (3) caching prompt prefixes (prompt caching, supported by Claude and GPT-4o). Smaller prompts = lower cost and lower latency, with minimal quality loss if trimming targets the least relevant content.
- *Why it matters here:* Sprint 6 adds context management improvements. Token optimization is not premature optimisation at this stage — it is a correctness constraint (staying within budget) and a cost control measure.
- *Interview:* "How would you reduce token cost in a production RAG system without degrading answer quality?"

---

## Build Tasks

### 1. Add Conversation Memory
- Maintain conversation history
- Pass relevant context to the LLM
- Limit memory to fit context window
- Experiment with memory strategies

### 2. Implement Structured Outputs
- Return responses as structured JSON
- Define response schemas
- Validate LLM output
- Handle invalid responses gracefully

### 3. Add Streaming Responses
- Enable streaming from the LLM
- Stream responses to the client
- Display tokens incrementally
- Handle stream completion and errors

### 4. Optimise Prompt Construction
- Separate System Prompt and User Prompt
- Reuse common prompt templates
- Reduce unnecessary context
- Experiment with prompt improvements

### 5. Improve Context Management
- Limit retrieved chunks based on token budget
- Remove duplicate context
- Prioritise high-ranking chunks
- Optimise context ordering

### 6. Build Conversation API
- Support multi-turn conversations
- Maintain session context
- Return structured responses
- Support streaming responses

### 7. Evaluate Agent Behavior
- Test multi-turn conversations
- Verify memory retention
- Validate structured outputs
- Measure response quality
- Measure latency improvements

---

## Concepts Learned

- Conversation Memory
- Context Window Management
- Structured Outputs
- JSON Schema Validation
- Streaming Responses
- Prompt Templates
- Token Optimisation
- Multi-turn Conversations

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- How does conversation memory work?
- What are the different types of memory in AI agents?
- Why use structured outputs?
- How do you validate LLM responses?
- How does streaming improve user experience?
- What is context window management?
- How do you optimise prompts?
- How do you manage long conversations?
