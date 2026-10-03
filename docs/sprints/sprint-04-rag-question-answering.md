# Sprint 4 — RAG Question Answering

## Goal

Build a complete Retrieval-Augmented Generation (RAG) pipeline that generates grounded answers
using the knowledge base built in previous sprints.

**Deliverable:** A working RAG application that answers Spring AI questions with relevant
citations.

---

## Build Tasks

### 1. Build the RAG Pipeline
- Connect Retrieval with the LLM
- Pass retrieved documents as context
- Generate grounded answers

### 2. Build Prompt Construction
- Create System Prompt
- Create User Prompt
- Inject retrieved context
- Define response guidelines
- Experiment with prompt variations

### 3. Integrate ChatModel
- Configure Spring AI `ChatModel`
- Send prompts to the LLM
- Parse AI responses
- Handle API failures

### 4. Implement Grounded Responses
- Restrict answers to retrieved context
- Prevent unsupported responses
- Handle "I don't know" scenarios

### 5. Add Source Citations
- Return source documents
- Return section/headings
- Return chunk references
- Format citations with the response

### 6. Build Ask API
- Create `/ask` endpoint
- Accept user questions
- Invoke Retrieval + LLM
- Return answer with citations

### 7. Test with Real Questions
- Ask Spring AI related questions
- Verify retrieved context
- Verify generated answers
- Verify citations
- Tune prompt if required

---

## Concepts Learned

- Retrieval-Augmented Generation (RAG)
- Prompt Engineering
- Context Injection
- Grounding
- Hallucination Reduction
- System Prompt vs User Prompt
- Context Window Management
- Citation Generation

---

## Sprint Outcome

```
     User Question
           │
           ▼
   Hybrid Retrieval
           │
           ▼
 Relevant Document Chunks
           │
           ▼
   Prompt Construction
           │
           ▼
  Spring AI ChatModel
           │
           ▼
Grounded Answer + Citations
```

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- What is RAG?
- How does a RAG pipeline work?
- Why is RAG better than a standalone LLM?
- How is retrieved context injected into the prompt?
- What causes hallucinations?
- How do you reduce hallucinations?
- Why are citations important?
- How many chunks should be retrieved?
