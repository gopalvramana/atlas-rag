# Sprint 8 — MCP Integration

## Goal

Expose Atlas as an MCP Server so AI clients like Claude Desktop and other MCP-compatible
applications can interact with it.

**Deliverable:** A working MCP Server that exposes Atlas capabilities as tools.

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
