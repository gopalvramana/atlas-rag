# Roadmap Gaps — What's Covered vs Missing

Checked against `docs/ai-engineer-roadmap.md` on 2026-10-04.

Legend: ✅ Covered · ⚠️ Partial · ❌ Missing

---

## 1. LLM Gateway / Model Platform (Sprint 6 expansion)

| Topic | Status | Notes |
|---|:---:|---|
| Model abstraction | ⚠️ | Mentioned briefly in Sprint 06 |
| Model routing | ✅ | Covered in Sprint 06 + diagram |
| Model fallback | ✅ | Listed in Sprint 06 and Sprint 09 |
| Provider failover | ❌ | Not mentioned |
| Rate limiting | ❌ | Not mentioned |
| Token budgets | ❌ | Not mentioned |
| Prompt management | ⚠️ | Prompt versioning in Sprint 09 only |
| Model/version management | ❌ | Not mentioned |
| Response caching | ✅ | Mentioned Sprint 06 and Sprint 09 |
| Cost optimization | ⚠️ | Token cost mentioned, not as a gateway concern |
| Latency optimization | ⚠️ | Mentioned in Sprint 09 only |
| API-key / secrets management | ⚠️ | Listed in Sprint 09 security only |
| Provider lock-in | ❌ | Not mentioned |
| Model outage handling | ⚠️ | In Sprint 09 reliability section only |
| Model capability routing | ❌ | Not mentioned |
| Principal questions (gateway design) | ❌ | Questions not included in Sprint 06 |

**Verdict:** Sprint 06 covers the diagram and high-level pattern. The deep LLM Gateway topic — with rate limiting, budgets, failover, lock-in, capability routing, and principal-level questions — is missing.

---

## 2. AI Security Architecture (expanded section)

| Topic | Status | Notes |
|---|:---:|---|
| Authentication and authorisation | ⚠️ | One bullet in Sprint 09 |
| Tenant isolation | ⚠️ | One bullet in Sprint 09 |
| PII detection and redaction | ⚠️ | One bullet in Sprint 09 |
| Data leakage prevention | ⚠️ | One bullet in Sprint 09 |
| Prompt injection | ⚠️ | One bullet in Sprint 09 |
| Indirect prompt injection | ❌ | Not mentioned |
| Jailbreak defences | ⚠️ | One bullet in Sprint 09 |
| Tool authorisation | ⚠️ | One bullet in Sprint 09 |
| Excessive agency | ❌ | Not mentioned |
| Unauthorized tool execution | ❌ | Not mentioned |
| Secrets management | ⚠️ | One bullet in Sprint 09 |
| Data access control | ❌ | Not mentioned |
| Authorization propagation (user → agent → API) | ❌ | Not mentioned |
| Auditability / AI decision logging | ⚠️ | Audit logging listed in Sprint 09 governance |
| Attack flow diagram (indirect prompt injection) | ❌ | Missing entirely |
| Principal-level security questions | ❌ | Not included |

**Verdict:** Sprint 09 has a bullet list. The expanded AI Security Architecture section — with indirect prompt injection, excessive agency, authorization propagation, attack flow diagram, and dedicated questions — is missing.

---

## 3. Event-Driven AI Architecture

| Topic | Status | Notes |
|---|:---:|---|
| Kafka + AI integration | ❌ | Not mentioned |
| Event-driven ingestion pipeline | ❌ | Not mentioned |
| Asynchronous embedding pipelines | ❌ | Not mentioned |
| Event-driven agent workflows | ❌ | Not mentioned |
| AI + enterprise APIs | ❌ | Not mentioned |
| Event replay | ❌ | Not mentioned |
| Idempotency | ❌ | Not mentioned |
| Retry / DLQ | ❌ | Not mentioned |
| Backpressure | ❌ | Not mentioned |
| Exactly-once vs at-least-once | ❌ | Not mentioned |
| Long-running AI workflows | ❌ | Not mentioned |
| Workflow state management | ❌ | Not mentioned |
| Kafka ingestion pipeline diagram | ❌ | Missing |
| Kafka → AI agent pipeline diagram | ❌ | Missing |
| Principal-level Kafka + AI questions | ❌ | Missing |

**Verdict:** Entirely missing. This section does not exist in the current roadmap.

---

## 4. AI System Design (interview phase)

| Topic | Status | Notes |
|---|:---:|---|
| Dedicated AI system design section | ❌ | Not present |
| Design exercise: Enterprise AI Support Agent | ❌ | Not present |
| Design exercise: Enterprise RAG Platform | ❌ | Not present |
| Design exercise: AI Log Analysis Platform | ❌ | Not present |
| Design exercise: AI Document Intelligence | ❌ | Not present |
| Design exercise: Agentic Customer Service | ❌ | Not present |
| Design exercise: Enterprise Knowledge Assistant | ❌ | Not present |
| Design exercise: AI Incident Management | ❌ | Not present |
| Design exercise: Multi-tenant AI Platform | ❌ | Not present |
| Design challenge template (requirements → failure modes) | ❌ | Not present |
| Sample challenge: 10M docs / 100K users support agent | ❌ | Not present |

**Verdict:** Entirely missing.

---

## 5. Updated Sprint Overview (Phase 10 + Phase 11)

| Item | Status | Notes |
|---|:---:|---|
| Phase 0–9 in roadmap table | ✅ | Present |
| Phase 10 — AI Security + Governance | ❌ | Not in table or as a sprint card |
| Phase 11 — AI System Design + Principal-Level Arch | ❌ | Not in table or as a sprint card |

**Verdict:** Phases 10 and 11 are missing from both the overview table and the sprint content.

---

## 6. Java + AI Architecture Positioning Section

| Item | Status | Notes |
|---|:---:|---|
| Layered architecture diagram (Java → AI Platform → AI/GenAI) | ⚠️ | `architecture-overview.svg` exists but doesn't show Kafka/IBM MQ/K8s/AWS layer explicitly |
| Explicit statement: "not replacing Java career with ML career" | ❌ | Not stated |
| AI / GenAI Layer listed (LLMs, RAG, Agents, MCP, Eval) | ⚠️ | Implied in cover, not as a named layer diagram |
| AI Platform Layer (Model Gateway, Vector DB, Guardrails, Obs, Gov) | ⚠️ | Sprint 06/07/09 cover the pieces, no unified layer diagram |
| Existing Strength layer (Java, Spring, Kafka, IBM MQ, AWS, K8s) | ⚠️ | Mentioned in cover text, not as a formal layer |

**Verdict:** The architecture-overview SVG covers the broad structure but doesn't name Kafka, IBM MQ, Kubernetes, or AWS, and the explicit positioning statement is missing.

---

## 7. Python Track

| Item | Status | Notes |
|---|:---:|---|
| Basic Python | ✅ | Present |
| FastAPI | ✅ | Present |
| LLM API integration | ✅ | Present |
| LangChain / LangGraph | ✅ | Present |
| Jupyter | ✅ | Present |
| Basic ML terminology | ✅ | Present |
| Basic data manipulation | ❌ | Not listed |
| OpenAI / Anthropic / Gemini APIs (explicitly listed) | ⚠️ | "Call LLM APIs (OpenAI, Anthropic)" — Gemini not named |
| "AI ecosystem interoperability language" positioning | ❌ | Not stated |
| Java = primary production language (explicit) | ❌ | Not stated |

**Verdict:** Mostly covered. Missing: data manipulation, Gemini, and the explicit positioning of Python as "AI ecosystem interoperability language."

---

## 8. Final Positioning Statement

| Item | Status | Notes |
|---|:---:|---|
| Current statement present | ✅ | Present at end of doc |
| New statement (Java/Spring/Cloud Architect specializing in enterprise AI) | ❌ | Old wording, not the new version |
| "AI integration and AI platforms" framing | ❌ | Not in current statement |
| "distributed enterprise systems, APIs, events and cloud infrastructure" | ❌ | Not in current statement |

**Verdict:** Statement exists but needs replacement with the new wording.

---

## 9. Final End-State Visual

| Item | Status | Notes |
|---|:---:|---|
| "20 Years of Enterprise Engineering → ... → Enterprise AI Architecture" flow | ❌ | Missing |
| Target titles: Principal AI Engineer / AI Integration Architect / AI Platform Architect | ❌ | Missing |
| "Does not need to become ML researcher" statement | ❌ | Missing |
| "Competitive advantage = enterprise engineering + AI system design" | ❌ | Missing |

**Verdict:** Entirely missing.

---

## Summary

| # | Section | Status |
|---|---|:---:|
| 1 | LLM Gateway deep expansion | ⚠️ Partial |
| 2 | AI Security Architecture (expanded) | ⚠️ Partial |
| 3 | Event-Driven AI Architecture | ❌ Missing |
| 4 | AI System Design (interview phase) | ❌ Missing |
| 5 | Phase 10 + Phase 11 in roadmap | ❌ Missing |
| 6 | Java + AI Architecture Positioning | ⚠️ Partial |
| 7 | Python track | ✅ Mostly done |
| 8 | Final positioning statement (new wording) | ⚠️ Partial |
| 9 | Final end-state visual | ❌ Missing |

### What to build next

**High impact, missing entirely:**
- Event-Driven AI Architecture section (Phase 11-adjacent) — needs 2 pipeline diagrams
- AI System Design section with 8 design exercises
- Phase 10 sprint card (AI Security Architecture) with attack-flow diagram
- Phase 11 sprint card (AI System Design)
- Final end-state visual / career-target diagram

**Quick fixes (partial → complete):**
- Sprint 06: add full LLM Gateway topic with provider failover, rate limiting, lock-in, capability routing, and principal questions
- Sprint 09: replace security bullet list with expanded AI Security Architecture
- `architecture-overview.svg`: add Kafka / IBM MQ / K8s / AWS to existing-strength layer
- Python section: add Gemini, data manipulation, explicit positioning
- Final positioning statement: replace with new wording
