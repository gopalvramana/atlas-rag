# Future Project: Customer Support Agent — Implementation Plan

**Status:** Planned, not started. Separate project, not part of Atlas RAG.

---

## Why this is a separate project

Atlas RAG is the learning project for **foundational RAG and agentic workflows**: ingestion,
hybrid retrieval, grounded answering, and a basic ReAct tool loop. Its scope stays fixed.

The Customer Support Agent is the practical project for **production-grade agent engineering**:
the capabilities a real agent needs once it acts on customer data and systems, not just answers
questions. Keeping them apart preserves a clear boundary between what Atlas teaches and what
this project teaches.

---

## How each capability is documented

Every capability below is filled in using the same five sections before any code is written
(design before code):

1. **Engineering objective:** what must be true when this capability is done.
2. **Customer-support use case:** the concrete support scenario that needs it.
3. **Architecture and design decisions:** components, trade-offs, and ADRs.
4. **Implementation tasks:** ordered, buildable steps.
5. **Test scenarios and acceptance criteria:** how we know it works.

---

## Gaps carried over from Atlas RAG

Known weaknesses in the Atlas Sprint 5 agent, left in Atlas on purpose and solved here.
Details: `docs/interview-prep.md`, Agent section, "Honest gaps".

| Atlas gap | Solved in capability |
|---|---|
| No cap on tool-call iterations per request | 8. Reliability |
| Context grows unmanaged across iterations (~29K prompt tokens for one 5-iteration question) | 6. Context management (fundamentals also in Atlas Sprint 6, Task 6) |
| Tool exceptions returned to the model but never logged; no retryable vs fatal split | 5. Failure handling |
| Raw external error text (GitHub response body) passed to the model | 4. Tool security |
| Tool-usage recording assumes tool calls run one at a time (plain `ArrayList`) | 2. Execution ordering |

---

## Capability Roadmap

### 1. Tool orchestration
LLM-driven and application-driven workflows.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 2. Execution ordering
Sequential, parallel and conditional execution.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 3. Human-in-the-loop
Approval before destructive or sensitive operations.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 4. Tool security
Authentication, authorization and permissions.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 5. Failure handling
Retries, timeouts and fallback strategies.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 6. Context management
Conversation history, token budgets and pruning.

> Builds on Atlas RAG Sprint 6, Task 6 (agent-loop context & token management), which covers
> the fundamentals: keeping the loop and memory inside the context window. This project goes
> further, to production-grade budgets, pruning and summarization policies.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 7. Observability
Execution traces, latency and token usage.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 8. Reliability
Prevent repeated calls and uncontrolled execution.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_

### 9. Evaluation
Test agent decisions and answer quality.

- **Engineering objective:** _TBD_
- **Customer-support use case:** _TBD_
- **Architecture and design decisions:** _TBD_
- **Implementation tasks:** _TBD_
- **Test scenarios and acceptance criteria:** _TBD_
