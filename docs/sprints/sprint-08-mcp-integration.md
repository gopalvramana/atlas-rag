# Sprint 8 — MCP Integration

## Goal

Expose Atlas as an MCP Server so AI clients like Claude Desktop and other MCP-compatible
applications can interact with it.

**Deliverable:** A working MCP Server that exposes Atlas capabilities as tools.

---

## LLM Fundamentals — Integrated into This Sprint

Learn this before writing any code. It explains the model–application boundary that MCP is built on.

**How an LLM-driven application discovers and invokes tools through MCP**
- *What:* The MCP server publishes a tool manifest — name, description, and input schema for each tool. An LLM client reads the manifest and presents tool descriptions to the model alongside the user message. The model generates a structured tool invocation request (which tool, which arguments). MCP transports the request to the server, executes the tool, and returns the result to the model. The model never calls the tool directly.
- *Why it matters here:* This is the same model–application boundary as Sprint 5 (tool calling), but now the tools are discovered dynamically via MCP rather than registered statically in the application. Understanding the boundary is what lets you explain why MCP is more than just a protocol wrapper.
- *Interview:* "How is tool invocation via MCP different from function calling registered statically in the application? What does MCP add?"

---

## Build Tasks

### 1. Learn MCP Architecture by Building
- Understand MCP Client and MCP Server
- Explore Spring AI MCP support
- Create Atlas MCP Server

### 2. Expose Atlas as an MCP Tool
- Create `askAtlas()` tool
- Connect tool to the RAG pipeline
- Return grounded answers
- Return citations

### 3. Expose Additional MCP Tools
- `searchDocs()`
- `compileJava()`
- `searchGithub()`
- Register all tools with the MCP Server

### 4. Configure MCP Server
- Configure transport
- Register tools
- Configure prompts
- Configure resources (if applicable)

### 5. Integrate with Claude Desktop
- Configure Claude Desktop
- Connect to Atlas MCP Server
- Verify tool discovery
- Test tool execution

### 6. Test End-to-End MCP Workflow
- Ask questions from Claude Desktop
- Verify tool invocation
- Verify RAG responses
- Verify citations
- Verify agent workflows

### 7. Document the MCP Integration
- Document architecture
- Document tool definitions
- Document setup steps
- Document sample interactions

---

## Concepts Learned

- Model Context Protocol (MCP)
- MCP Client
- MCP Server
- MCP Tools
- MCP Resources
- Tool Discovery
- AI-to-AI Integration
- Spring AI MCP

---

## Interview Readiness

After completing this sprint, you should be able to explain:

- What is MCP?
- Why was MCP introduced?
- How is MCP different from REST APIs?
- What are MCP Clients and MCP Servers?
- How do AI applications discover tools?
- How does Spring AI support MCP?
- How can Claude Desktop interact with an MCP Server?
- When should MCP be used instead of a traditional API?
