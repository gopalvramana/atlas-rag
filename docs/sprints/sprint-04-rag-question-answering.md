# Sprint 4 — RAG Question Answering

## Goal

Take the `RetrievedContext` from Sprint 3 and turn it into a grounded, cited LLM response.

The core engineering challenge in this sprint is **LLM-side context management** — not just
passing chunks to the LLM, but assembling a prompt that fits within the model's context window,
reserves space for the output, avoids wasting tokens on duplicate or low-value content, and
degrades gracefully when the retrieved context is too large.

**Deliverable:** A `RagService` that accepts a user question + `RetrievedContext` and returns
a structured `RagResponse` (answer + citations). Wired into `POST /api/v1/ask`.

```
User Question
      ↓
  RetrievedContext  (from Sprint 3)
      ↓
  Prompt Assembler
  ├── System prompt
  ├── User question
  ├── Retrieved chunks  ← token budget enforced here
  └── Output token reserve
      ↓
  Token Budget Check
  ├── Fits → send to LLM
  └── Overflow → trim / summarise / reject lowest-ranked chunks
      ↓
  Spring AI ChatModel
      ↓
  RagResponse
  ├── Answer
  └── Citations (source_file, version, chunk_index)
```

---

## LLM Fundamentals — Integrated into This Sprint

Learn these before writing any code. Each concept explains a decision you are about to make.

**Context window vs actual available context**
- *What:* The model's context window (e.g., 128K tokens for Claude, 16K for GPT-3.5) is the hard upper limit. The *available* context for retrieved chunks = window − system prompt − user question − output token reserve. The gap between window size and available context is often 1,000–3,000 tokens.
- *Why it matters here:* `PromptAssembler` must account for all budget consumers — not just the chunks. Ignoring system prompt tokens causes silent overflow.
- *Interview:* "What is the difference between the model's context window and the context available for retrieved chunks?"

**Input token budget vs output token reserve**
- *What:* If you fill the entire context window with input, the model has no room to generate output and will either truncate or return an API error. Output token reserve = the number of tokens you protect for the completion. For RAG answers, 1,000–2,000 tokens is typical.
- *Why it matters here:* `PromptAssembler` must subtract the output reserve before selecting chunks. This is a hard constraint, not a suggestion.
- *Interview:* "Why do you need to reserve tokens for output? What happens if you don't?"

**System prompt vs user prompt vs retrieved context**
- *What:* System prompt = persistent instructions + persona, processed with highest influence, placed first. User prompt = the question. Retrieved context = injected after the question (or in a separate user turn). Placement affects the model's attention weighting — instructions in the system prompt are more reliably followed than identical instructions in the user prompt.
- *Why it matters here:* The grounding instruction ("answer only from the context below") must be in the system prompt to be reliable. Retrieved chunks follow after the question.
- *Interview:* "Where do you place retrieved context in the prompt? Why does placement matter?"

**Temperature and top-p**
- *What:* Temperature (0–1+) scales the probability distribution over next tokens. 0 = always pick the highest-probability token (deterministic). 1 = sample proportionally (creative, variable). Top-p restricts sampling to tokens summing to probability mass p. For RAG, use temperature 0–0.2: factual, grounded, low hallucination risk.
- *Why it matters here:* `ChatModel` configuration should set low temperature for RAG answers. High temperature increases the chance the model generates plausible-sounding content not in the context.
- *Interview:* "What temperature would you use for a RAG system and why?"

**Why LLMs produce incorrect or unsupported answers**
- *What:* Three distinct failure modes: (1) context doesn't contain the answer → model guesses from training data; (2) context contains conflicting information → model picks one arbitrarily; (3) attention dilution in long contexts → model ignores parts of the context ("lost in the middle" problem). The grounding system prompt eliminates (1) with a cooperative model but cannot prevent (2) or (3).
- *Why it matters here:* Understanding the failure modes determines what the system prompt can and cannot fix, and informs the "cannot answer" fallback design.
- *Interview:* "You told the model to answer only from context but it still hallucinated. What could cause that?"

**Transformer and attention — conceptual understanding**
- *What:* The transformer processes the entire input sequence in parallel. Attention allows every token to "look at" every other token. In long inputs, attention is spread across more tokens — relevance signals from the beginning and middle of a long context can be diluted. This is why chunk ordering within the prompt matters.
- *Why it matters here:* The highest-relevance chunks should be placed first (and last) in the context block — not buried in the middle. This is an implementation decision in `PromptAssembler`.
- *Interview:* "Why does the order of retrieved chunks in the prompt affect answer quality?"

---

## Why LLM-Side Context Management Is the Hard Part

Sprint 3 already enforced a token budget at the retrieval stage. Sprint 4 enforces it again
at the prompt stage — because the system prompt and question also consume tokens.

| Budget component | Typical allocation |
|---|---|
| System prompt | ~300–500 tokens |
| User question | ~50–200 tokens |
| Retrieved context | whatever is left after the above and the output reserve |
| Output token reserve | ~1,000–2,000 tokens |
| **Total model context window** | e.g. 128,000 (Claude), 16,385 (GPT-3.5) |

Getting this wrong causes one of two failures:
- **Context overflow** — the API call is rejected or silently truncated
- **Wasted context** — sending duplicate or low-relevance chunks burns tokens and raises cost

---

## Build Steps

### Step 4.1 — PromptAssembler

Assembles the full prompt from its parts and enforces the token budget.

Responsibilities:
- Accepts: system prompt template, user question, `RetrievedContext`, model config
- Counts tokens for each part using jtokkit
- Calculates available context budget = model limit − system − question − output reserve
- Selects chunks from `RetrievedContext` in rank order until budget is exhausted
- Deduplicates: drops chunks whose content overlaps >80% with an already-selected chunk
- Returns: a `AssembledPrompt` with the final messages and a token breakdown

### Step 4.2 — Context overflow handling

Three strategies — implement in order of complexity:

| Strategy | When to use | How |
|---|---|---|
| Trim | Context slightly over budget | Drop the lowest-ranked chunks until it fits |
| Summarise | Context significantly over budget | Not in this sprint — placeholder for later |
| Reject | No chunks fit at all | Return a structured "cannot answer" response |

### Step 4.3 — RagService

Orchestrates the full flow: retrieval result → prompt assembly → LLM call → response parsing.

```java
RagResponse ask(String question, RetrievedContext context);
```

### Step 4.4 — Spring AI ChatModel integration

Configure and call the LLM via Spring AI `ChatModel`.
Parse the response into a `RagResponse`:
- `answer` — the LLM's text
- `citations` — list of `{source_file, version, chunk_index}` for each chunk used
- `tokenUsage` — prompt tokens, completion tokens, total

### Step 4.5 — Grounding and hallucination control

System prompt instructs the LLM to:
- Answer only from the provided context
- Say "I don't have enough information" if the context does not support an answer
- Cite the source file and version for each claim

### Step 4.6 — POST /api/v1/ask endpoint

```
POST /api/v1/ask
{ "question": "How do I configure ChatClient in Spring AI?" }

→ 200 OK
{
  "answer": "...",
  "citations": [
    { "sourceFile": "chat-client.adoc", "version": "1.0.0", "chunkIndex": 4 }
  ],
  "tokenUsage": { "prompt": 2140, "completion": 380, "total": 2520 }
}
```

### Step 4.7 — Test with real questions

Run 10 questions. For each verify:
- Was the right context retrieved?
- Did the answer stay within the retrieved context (no hallucination)?
- Are citations accurate — does the cited chunk actually support the claim?
- What was the token usage?

---

## Classes to Build

| Class | Module | Responsibility |
|---|---|---|
| `PromptAssembler` | `atlas-agent` | Builds final prompt; enforces token budget; deduplicates |
| `RagService` | `atlas-agent` | Orchestrates retrieval → prompt → LLM → response |
| `RagResponse` | `atlas-agent` | Answer + citations + token usage |
| `AskController` | `atlas-api` | `POST /api/v1/ask` REST endpoint |

---

## Concepts Learned

- Prompt assembly — system prompt vs user prompt vs context
- Token budgeting — how to allocate the context window across prompt components
- Output token reservation — why you must reserve space for the completion
- Context deduplication — why overlapping chunks waste the budget
- Context overflow — how to detect and handle it
- Hallucination — what causes it and how the system prompt constrains it
- Grounding — tying every claim to a source chunk
- Citations — how to link a response back to specific chunks

---

## Interview Readiness

**The key question:**
> "How do you manage the context window in a RAG system?"

Full answer: the context window is shared between system prompt, user question, retrieved
chunks, and output. You must account for all parts. Retrieved chunks are selected in rank
order until the remaining budget (after reserving space for the output) is exhausted.
Duplicate chunks are dropped. If the context overflows, you trim from the bottom of the
ranked list. If nothing fits, you return a structured "cannot answer" response rather than
hallucinating.

**Other questions:**
- What is the difference between system prompt and user prompt?
- How much of the context window should you reserve for output?
- How do you prevent the LLM from hallucinating in a RAG system?
- What does it mean for a response to be "grounded"?
- How do you generate citations in a RAG system?
- What happens if retrieved context contradicts itself?
- How does token cost relate to context window usage?
