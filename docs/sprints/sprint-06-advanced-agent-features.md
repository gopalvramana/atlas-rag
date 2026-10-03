# Sprint 6 — Advanced Agent Features

## Goal

Enhance the AI Agent with advanced capabilities such as memory, structured outputs, streaming,
and prompt optimisation to make it more intelligent, reliable, and user-friendly.

**Deliverable:** A conversational AI Agent that supports memory, streaming responses, and
structured outputs.

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
