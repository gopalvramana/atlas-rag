# Sprint 6 — Advanced Agent Features

## Goal

Enhance the AI Agent with model routing, streaming, memory, structured outputs, and prompt and
context optimisation, to make it more capable, cost-aware, reliable, and user-friendly.

**Deliverable:** A conversational AI Agent that routes each request to an appropriate model and
supports streaming responses, conversation memory, and structured outputs.

**Scope boundary:** Atlas stays focused on foundational RAG and agentic capabilities. Everything
here is built at learning depth. Production-grade concerns (provider failover, enterprise
gateway features, production context policies) belong to the Customer Support Agent project
(`docs/future-projects/customer-support-agent-implementation-plan.md`, ADR-024).

---

## LLM Fundamentals — Integrated into This Sprint

Learn these before writing any code. Each concept explains a decision you are about to make,
and names the build task that uses it.

**Model routing based on capability, latency, and cost**
- *What:* Route each request to the cheapest model that reliably handles it. Simple factual RAG lookups → a small model (GPT-4o-mini / Haiku). Multi-step reasoning and complex tool orchestration → a larger model (GPT-4o / Sonnet). A router inspects the request (endpoint, query shape, explicit selection) before choosing the model.
- *Why it matters here:* Task 1 implements basic routing. Knowing the capability/cost profile of each model is what makes a routing rule defensible rather than arbitrary.
- *Used in:* Task 1 (routing rule, explicit selection), Task 9 (routed vs fixed-model comparison).
- *Interview:* "How would you implement model routing? What signals would you use to decide which model handles a request?"

**Small vs large models**
- *What:* Larger models have higher capability on complex tasks, slower inference, and higher cost per token. Smaller models are faster and cheaper but may fail at multi-step reasoning, nuanced instruction following, or reliable structured output. The right model is the smallest one that meets quality requirements for the task class.
- *Why it matters here:* Task 1 configures one small and one large model and measures the difference against a fixed-model baseline. Task 7 validates structured output. If the small model keeps producing invalid JSON, check whether it's too small before tuning the schema.
- *Used in:* Task 1 (two model configurations, baseline comparison), Task 7 (structured-output reliability per model).
- *Interview:* "When would you use a smaller model in a RAG system? What are the failure modes of smaller models?"

**Hosted vs self-hosted models**
- *What:* Hosted APIs (OpenAI, Anthropic, Google) = no GPU infrastructure, simple SDK integration, but data leaves your environment, subject to provider pricing and uptime. Self-hosted (Llama 3, Mistral) = full data control, no per-token cost at inference time, but GPU provisioning, model serving, version management, and performance tuning become your responsibility.
- *Why it matters here:* Task 1's gateway keeps model calls provider-independent. That only pays off if you understand what swapping providers, including to a self-hosted model, would actually involve.
- *Used in:* Task 1, as a design input only. Atlas uses hosted models. Self-hosting is **not implemented** in this sprint.
- *Interview:* "An enterprise customer requires that no data leaves their VPC. How does this affect your model selection and deployment architecture?"

**Quality, latency, cost, and operational complexity trade-offs**
- *What:* These four dimensions always trade off. Higher quality = larger model = higher cost and latency. Lower cost = smaller model = lower quality. Self-hosting = lower per-token cost at scale but adds significant operational complexity. There is no configuration that optimises all four simultaneously.
- *Why it matters here:* Every routing decision is this trade-off made explicit. Task 1 captures usage, latency, and tokens per request so the trade-off can be measured, not just argued.
- *Used in:* Task 1 (usage, latency, and token capture; baseline comparison), Task 9 (final measurement).
- *Interview:* "How would you present the model selection decision to an engineering leadership audience?"

**Token optimization and context reduction**
- *What:* Reduce prompt size by: (1) trimming low-relevance chunks; (2) summarising conversation history rather than passing all turns; (3) caching prompt prefixes (prompt caching, supported by Claude and GPT-4o). Smaller prompts = lower cost and lower latency, with minimal quality loss if trimming targets the least relevant content.
- *Why it matters here:* Token optimization isn't premature at this stage. It's a correctness constraint (staying within budget) and a cost control.
- *Used in:* Task 3 (reduce unnecessary prompt context), Task 4 (chunk budget, deduplication), Task 5 (memory within the context window), Task 6 (agent-loop growth, tool-result truncation, history pruning, token budgets). Prompt caching (3) is **not implemented** in this sprint. It's tracked separately as `docs/PLAN.md` §3 step 8.
- *Interview:* "How would you reduce token cost in a production RAG system without degrading answer quality?"

---

## Build Tasks

Order follows dependencies. The gateway comes first because every later task makes LLM calls
through it. Single-turn prompt and context work comes before multi-turn memory, because memory
has to fit inside the same context budget. Agent-loop token management follows memory, because
it includes pruning and summarising that history.

### 1. Add Model Gateway and Model Routing

Introduce a thin, provider-independent gateway that every LLM call in Atlas goes through, and
route each request to one of two model configurations.

- Introduce a provider-independent model gateway abstraction. Build on Spring AI's existing
  abstraction (`ChatClient` / `ChatModel` with portable `ChatOptions`) rather than a new
  framework. Don't use provider-specific options types in routing code.
- Configure at least two model configurations with different capability and cost profiles
  (e.g. `fast` → `gpt-4o-mini`, `capable` → `gpt-4o`), bound via `@ConfigurationProperties`.
- Implement basic model routing based on request complexity and capability, using a simple,
  deterministic rule (e.g. by endpoint, `/ask` vs `/agent`, plus simple query signals).
- Support explicit model selection where appropriate. A caller may request a configuration by
  name. The name is validated against a whitelist of configured names, never accepted as a
  raw model ID.
- Capture model usage, latency, and token consumption for every request: configuration name,
  model ID, routing reason, latency, and prompt, completion, and total tokens.
- Compare routing decisions against a fixed-model baseline.

**Acceptance criteria**
- `RagService` and `AtlasAgent` call the LLM only through the gateway. No other class chooses a
  model.
- Model IDs live only in configuration. Changing a model needs no code change.
- The routing rule is deterministic and unit-tested without calling an LLM.
- Explicit selection works for valid names. An unknown name returns HTTP 400 and is logged.
- Every request logs configuration, model ID, routing reason, latency, and token usage.
  `/agent` responses report token usage, as `/ask` responses already do.
- The same question set (the 10 Sprint 4 questions plus the 3 Sprint 5 agent questions) is run
  three ways: fixed `fast`, fixed `capable`, and routed. The results are recorded in a table:
  correctness, total tokens, latency, and estimated cost. Per-token prices come from
  configuration, not code.
- The comparison states whether routing beat the fixed baseline, and on which dimension.

**Out of scope (keep the gateway bounded)**
- Provider failover, multi-provider load balancing, response caching, and an admin UI. These
  are production gateway concerns for the Customer Support Agent project.
- Classifier- or LLM-based routing. Rule-based routing is enough to learn the trade-off.
- Switching models between iterations of the agent loop. Spring AI manages the loop
  internally, so routing applies per request.
- Token-budget enforcement. It's added to the gateway in Task 6, after streaming exists
  (ADR-023).
- Inbound API rate limiting (Sprint 9, ADR-023).

**Open design decisions (agree before code)**
- **Gateway shape:** an Atlas `ModelGateway` class wrapping `ChatClient`, or a Spring AI
  advisor that applies routing and records usage. An advisor reuses the framework's extension
  point. A class is easier to unit-test and to explain.
- **Second configuration:** a second OpenAI model (no new dependency) or a second provider such
  as Anthropic (genuinely tests provider independence, but adds a starter and an API key).
- **Routing rule:** which signals to use beyond endpoint (`/ask` vs `/agent`), e.g. query
  length or code-generation intent, and which configuration each signal maps to.
- **Agent-loop usage:** check whether Spring AI's `ChatResponse` usage is cumulative across
  tool-calling iterations, or only covers the last call, before reporting `/agent` token
  usage.
- **`docs/PLAN.md` §2.6:** it describes a per-step split (small model for tool calls, large
  model for the final answer). Per-request routing can't do that with Spring AI's managed
  loop. Decide whether to revise the plan or treat per-step routing as a known limitation.

### 2. Add Streaming Responses
- Enable streaming from the LLM through the gateway
- Stream responses to the client (`GET /api/v1/ask/stream`, SSE; spec in `docs/PLAN.md` §2.5)
- Display tokens incrementally
- Handle stream completion and errors
- Record the same usage, latency, and token data as non-streaming calls (Task 1)

### 3. Optimise Prompt Construction
- Separate System Prompt and User Prompt
- Reuse common prompt templates
- Reduce unnecessary context
- Experiment with prompt improvements

### 4. Improve Context Management
- Limit retrieved chunks based on token budget
- Remove duplicate context
- Prioritise high-ranking chunks
- Optimise context ordering

### 5. Add Conversation Memory
- Maintain conversation history
- Pass relevant context to the LLM
- Limit memory to fit context window
- Experiment with memory strategies

### 6. Agent-loop Context & Token Management

Unfinished topic carried over from Sprint 5. The ReAct loop works, but its context growth
is unmanaged (one 5-iteration multi-tool question consumed ~29K prompt tokens).

- Context accumulation across multiple LLM/tool interactions.
- Token consumption by tool outputs.
- Context-window limit handling.
- Tool-result truncation and filtering.
- Conversation-history pruning and summarization.
- Per-request LLM token budgets, enforced at the gateway from Task 1 (ADR-023).

**Status:** Not implemented — pending dedicated learning and implementation.

Related: Task 5 (memory must fit the context window) and ADR-023 (token budgets) in `docs/decisions.md`.

**Scope boundary:** Atlas covers the fundamentals only. Production-grade budgets, pruning and
summarization policies continue in the Customer Support Agent project, capability 6
(`docs/future-projects/customer-support-agent-implementation-plan.md`). See ADR-024.

### 7. Implement Structured Outputs
- Return responses as structured JSON
- Define response schemas
- Validate LLM output
- Handle invalid responses gracefully
- Record the validation failure rate per model configuration (links to Task 1)

### 8. Build Conversation API
- Support multi-turn conversations
- Maintain session context
- Return structured responses
- Support streaming responses

### 9. Evaluate Agent Behavior
- Test multi-turn conversations
- Verify memory retention
- Validate structured outputs
- Measure response quality
- Measure latency improvements
- Re-run the Task 1 routed vs fixed-model comparison on the finished Sprint 6 build

---

## Concepts Learned

- Model Gateway
- Model Routing
- Routing Strategies (rule-based, explicit selection; classifier-based as a known next step)
- Model Selection Trade-offs (quality, latency, cost)
- Model Evaluation against a Fixed-Model Baseline
- Streaming Responses
- Prompt Templates
- Context Window Management
- Conversation Memory
- Agent-loop Token Management
- Token Optimisation
- Structured Outputs
- JSON Schema Validation
- Multi-turn Conversations

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- What is a model gateway, and why put one in front of the LLM?
- How does model routing work, and what signals decide which model handles a request?
- What routing strategies exist (rule-based, classifier-based, LLM-based, explicit selection),
  and when is each worth its complexity?
- How do you prove routing helped? What does a fixed-model baseline comparison show?
- How do you choose between a small and a large model for a task?
- How does streaming improve user experience?
- How does conversation memory work?
- What are the different types of memory in AI agents?
- What is context window management?
- How do you optimise prompts?
- How do you manage long conversations?
- Why use structured outputs?
- How do you validate LLM responses?
