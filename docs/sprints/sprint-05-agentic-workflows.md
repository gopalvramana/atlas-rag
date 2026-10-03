# Sprint 5 — Agentic Workflows

## Goal

Transform Atlas from a RAG application into an AI Agent that can decide which tools to use
before generating an answer.

**Deliverable:** A working AI Agent capable of reasoning, invoking tools, and generating
grounded responses.

---

## Build Tasks

### 1. Build the Agent
- Create the Atlas Agent
- Implement the agent execution flow
- Integrate with the existing RAG pipeline

### 2. Create searchDocs Tool
- Convert the Retrieval service into a tool
- Return relevant document chunks
- Integrate with the Agent

### 3. Create compileJava Tool
- Accept Java source code
- Compile the code
- Return compilation errors or success
- Integrate with the Agent

### 4. Create searchGithub Tool
- Search GitHub Issues
- Search GitHub Pull Requests
- (Optional) Search repository source code
- Integrate with the Agent

### 5. Implement Tool Calling
- Allow the LLM to decide which tool to invoke
- Execute the selected tool
- Return tool results to the LLM
- Generate the final response

### 6. Support Multi-Step Reasoning
- Execute multiple tools when required
- Pass output from one tool to another
- Generate the final answer after all tool executions

### 7. Build Agent API
- Create `/agent` endpoint
- Accept user questions
- Return final response
- Include tools used during execution

### 8. Add Agent Logging
- Log user question
- Log selected tools
- Log tool outputs
- Log final response

---

## Concepts Learned

- Agentic AI
- Tool Calling
- Function Calling
- ReAct Pattern
- Multi-Step Reasoning
- Tool Orchestration
- Agent Execution Loop

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- What is an AI Agent?
- Difference between RAG and an AI Agent
- What is Tool Calling?
- What is the ReAct pattern?
- How does an agent decide which tool to use?
- How are multiple tools orchestrated?
- How do you debug an AI Agent?
