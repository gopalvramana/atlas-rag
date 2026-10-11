# Sprint 5 — Agentic Workflows

## Goal

Transform Atlas from a RAG application into an AI Agent that can decide which tools to use
before generating an answer.

**Deliverable:** A working AI Agent capable of reasoning, invoking tools, and generating
grounded responses.

---

## LLM Fundamentals — Integrated into This Sprint

Learn these before writing any code. Each concept explains a decision you are about to make.

**Tool-use and reasoning-capable models**
- *What:* Not all models support tool calling. A model must be explicitly capable of generating structured tool invocation requests (function calling / tool use). GPT-4o, Claude 3+ Sonnet/Opus, and Gemini 1.5+ are tool-use capable. Smaller or older models may silently fail to generate valid tool calls.
- *Why it matters here:* Atlas's agent depends on the model returning a valid tool invocation JSON. Choosing a model that does not reliably support this breaks the agent loop before you can even debug retrieval.
- *Interview:* "How do you choose a model for an agentic workflow? What happens if the model doesn't support tool calling?"

**How models select and request tools**
- *What:* The application sends the model a list of tool schemas (name, description, input parameters as JSON Schema). The model reads the descriptions and generates a structured tool call request — it picks the tool and populates the arguments. The model cannot execute anything directly; it only outputs a request.
- *Why it matters here:* Tool descriptions are the interface between the application and the model. A vague or ambiguous description causes the model to select the wrong tool or generate invalid parameters.
- *Interview:* "How does the model decide which tool to call? What makes a good tool description?"

**Model-generated tool requests vs actual tool execution**
- *What:* The model outputs a tool invocation specification. The application intercepts it, validates it, executes the actual tool (search, compile, API call), and returns the result. The model never directly calls external systems — this is a deliberate security boundary.
- *Why it matters here:* Authorization checks, input validation, and rate limiting all belong in the application's tool execution layer — not in the model's output. The model cannot be trusted to self-authorize.
- *Interview:* "Where does authorization for tool execution happen in an agentic system?"

**Model capability trade-offs for agent workflows**
- *What:* Reasoning-capable models (o1, Claude 3.5 Sonnet) are better at multi-step tool planning, decomposing complex tasks, and recovering from tool failures. They are slower and more expensive. For simple single-tool lookups, a lighter model (Haiku, GPT-4o-mini) is sufficient and significantly cheaper.
- *Why it matters here:* The Atlas agent in this sprint uses simple tool orchestration. A mid-tier model is appropriate. Over-provisioning on model capability adds latency and cost without quality benefit at this stage.
- *Interview:* "How do you decide which model to use for an agent that uses three different tools?"

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
