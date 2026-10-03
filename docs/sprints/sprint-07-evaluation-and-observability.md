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

## Interview Readiness

After completing this sprint, you should be able to explain:

- How do you evaluate a RAG system?
- How do you measure retrieval quality?
- How do you evaluate LLM responses?
- What are common RAG evaluation metrics?
- How do you detect hallucinations?
- Why is observability important in AI applications?
- How do you monitor token usage and latency?
