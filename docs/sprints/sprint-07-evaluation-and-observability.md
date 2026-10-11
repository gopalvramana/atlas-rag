# Sprint 7 — Evaluation & Observability

## Goal

Build an evaluation framework to measure the quality, accuracy, and performance of Atlas.

**Deliverable:** An automated evaluation framework that validates retrieval quality, answer
quality, citations, and overall agent performance.

---

## Build Tasks

### 1. Create an Evaluation Dataset
- Prepare sample questions
- Define expected answers
- Define expected citations
- Categorise questions by topic

### 2. Build an Evaluation Runner
- Execute evaluation dataset
- Run each question through Atlas
- Capture responses
- Store evaluation results

### 3. Evaluate Retrieval Quality
- Verify retrieved chunks
- Measure retrieval accuracy
- Compare retrieval strategies
- Tune retrieval parameters

### 4. Evaluate Response Quality
- Compare generated answers with expected answers
- Measure answer relevance
- Measure completeness
- Identify hallucinations

### 5. Validate Citations
- Verify source documents
- Verify referenced sections
- Detect missing or incorrect citations

### 6. Add Observability
- Log retrieval latency
- Log LLM response time
- Log token usage
- Log tool execution time
- Log overall request duration

### 7. Generate Evaluation Report
- Total questions evaluated
- Retrieval accuracy
- Response accuracy
- Citation accuracy
- Average response time
- Token consumption

---

## Concepts Learned

- LLM Evaluation
- Retrieval Evaluation
- Answer Evaluation
- Citation Validation
- AI Observability
- Latency Measurement
- Token Usage Analysis
- Hallucination Detection

---

## LLM Fundamentals — Integrated into This Sprint

**Evaluating model quality and consistency**
- *What:* LLMs are non-deterministic at temperature > 0. The same prompt produces different outputs across runs. Evaluation must aggregate metrics across multiple runs, or use temperature 0 for deterministic scoring. A single-run evaluation of a non-deterministic model is not statistically valid.
- *Why it matters here:* The evaluation runner should use a fixed temperature (0 or a low value) for reproducibility, and distinguish between retrieval failures and model generation failures — the failure modes require different fixes.
- *Interview:* "How do you design a statistically valid evaluation for a non-deterministic LLM system?"

**Why model changes affect retrieval-grounded answers and tool use**
- *What:* Changing the model provider, version, or generation parameters changes: how instructions in the system prompt are followed, how the model weighs context vs its pre-training, how reliably it generates structured output, and which tools it selects. An answer that was grounded before may hallucinate after a model change, even with identical retrieval.
- *Why it matters here:* Model version changes are a first-class change event requiring a full evaluation run. This must be enforced in the evaluation pipeline, not treated as a "minor update."
- *Interview:* "You upgraded from Claude 3 Sonnet to Claude 3.5 Sonnet. What must you do before releasing to production?"

**Regression evaluation when prompts, models, or context strategies change**
- *What:* Any change to system prompt wording, context placement, model selection, chunk size, or reranking strategy can degrade quality on previously passing test cases while improving others. Regression evaluation against a fixed dataset of expected outputs is the only way to detect this.
- *Why it matters here:* The 50-question evaluation dataset exists precisely for this. Sprint 9 will add CI gates that block deployment when evaluation scores drop below threshold.
- *Interview:* "How do you ensure that a prompt change doesn't degrade RAG quality on existing query types?"

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- How do you evaluate a RAG system?
- How do you measure retrieval quality?
- How do you evaluate LLM responses?
- What are common RAG evaluation metrics?
- How do you detect hallucinations?
- Why is observability important in AI applications?
- How do you monitor token usage and latency?
