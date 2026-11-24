# Interview Prep — Atlas RAG

Q&A grounded in exactly what was built, added module by module as each one is finished
(see `CLAUDE.md` commit rule). Don't write these from memory or theory — write them right
after building the thing, while the trade-offs are fresh.

The previous attempt at this project produced a full Q&A set for the ingestion pipeline
before it was rebuilt here — it's preserved in `docs/PLAN.md` Appendix as reference
material. Once `atlas-ingestion` is rebuilt, re-verify each answer still matches the actual
code (don't just copy it over) and move it into this file.

---

## Ingestion — pending rebuild verification

## Retrieval — pending

## Agent (Sprint 5)

Built: `AtlasAgent`, `RecordingToolCallback`, `SearchDocsTool`, `CompileJavaTool`,
`SearchGithubTool`, `AgentController` (`POST /api/v1/agent`).

### Concepts

**Q: What's the difference between `/ask` (RAG) and `/agent`?**

`/ask` is a fixed pipeline. The code always retrieves, builds one prompt, and makes one LLM
call. `/agent` hands control to the model. It decides whether to search the docs, search
GitHub, compile code, or answer directly, and it can loop through several tool calls before
answering. RAG is predictable and cheap. The agent is flexible but costs more and is harder
to predict. In Atlas, searching the docs is just one tool the agent can choose.

**Q: What is the ReAct pattern, and how does it run in Atlas?**

ReAct stands for Reason + Act. The model reasons about what it needs, calls a tool (acts),
reads the result, and repeats until it can answer. In Atlas, I don't write that loop myself.
I register tools with `ChatClient` via `.toolCallbacks(...)` and call `.call()`. Spring AI
sends the tool definitions with the prompt. When the model replies with a tool-call request,
Spring AI runs the matching Java method, appends the result to the conversation, and calls
the model again. This repeats until the model returns plain text.

**Q: How does the model know which tool to use?**

Only from text. Spring AI turns each `@Tool` method into a JSON Schema definition: a name, the
`@Tool` description, and the `@ToolParam` descriptions. The system prompt also explains when
to use each tool. So tool descriptions act as routing logic. A vague description causes wrong
tool choices, and better wording is the first fix to try. For example, `@ToolParam` on
`searchDocs` tells the model to "rephrase the user question as a focused search query"
instead of passing the raw question.

### Design decisions

**Q: Why let Spring AI run the loop instead of writing your own?**

It's less code, and it's the framework's tested path for multi-turn tool calling. The
trade-off is control. I get no hook between iterations, so there's no built-in cap on
iterations and no way to trim context between turns. That gap showed up in testing (see the
29K-token question below) and is tracked as Sprint 6, Task 6.

**Q: How do you know which tools the agent used? Why a decorator?**

`RecordingToolCallback` implements `ToolCallback` and wraps the real callback. On each `call()`
it adds the tool name to a list, then delegates. `AtlasAgent.ask()` creates a new list per
request, wraps the three tools, and returns the list in `AgentResponse.toolsUsed`.

I used a decorator because recording is a cross-cutting concern:
- The tools stay unchanged and only do their own job (single responsibility).
- Adding a new tool needs no recording code (open/closed).
- The wrapper can stand in anywhere a `ToolCallback` is expected (Liskov substitution).

Because the list is per request, concurrent requests never share it. Grepping the
`SimpleLoggerAdvisor` logs would work for debugging, but it can't return data to the API
caller.

**Q: Why do tools return errors as results instead of throwing?**

The model can only act on what it reads. `CompileJavaTool` returns a
`CompileResult(success, errors)` with line and column details. `SearchGithubTool` returns an
error `GitHubResult` on a non-200 response. A thrown exception just ends the loop. An error
returned as data lets the model fix its input and try again, which is exactly what happened
in testing.

**Q: Isn't letting an LLM compile arbitrary code dangerous?**

Compiling isn't executing. `CompileJavaTool` uses `javax.tools.JavaCompiler` in-process, writes
the source to a temp directory, collects diagnostics, and deletes the files afterwards. It
never loads or runs the compiled class. All three tools are read-only. The checks live in
the tool layer, not the prompt, because the model can't be trusted to police itself. The tool
needs a JDK, not a JRE, so the constructor fails fast if no compiler is available.

### What testing showed

**Q: What did you observe when the agent ran?**

- **Parallel tool calls:** in a single turn, the model requested `searchDocs` and
  `searchGithub` together. "Parallel" means one model response asked for both, not that they
  necessarily ran concurrently.
- **Self-correction:** `compileJava` failed and the model tried 3 times. Each attempt used the
  compiler errors from the previous one.
- **Full trace:** `toolsUsed` captured every iteration in order:
  `[searchDocs, searchGithub, compileJava, compileJava, compileJava]`.
- **Context growth:** that 5-iteration question used about 29K prompt tokens.

**Q: Why did one question use 29K tokens?**

Every iteration resends the whole conversation so far: the system prompt, the question, every
earlier tool call, and every tool result. One `searchDocs` result alone can be up to the
retrieval token budget (6,000 tokens). So cost grows with each iteration, not just each
question. Without a cap, a long tool loop can exceed the context window or blow the budget.
The fixes are tool-result truncation, history pruning, and per-request token budgets.
They're Sprint 6, Task 6, and the budgets are ADR-023.

### Honest gaps (what I'd change)

**Q: What would you improve in this agent?**

These gaps are known and intentionally left in Atlas. They're solved in the follow-on
Customer Support Agent project (`docs/future-projects/customer-support-agent-implementation-plan.md`,
capability number in brackets). Context growth is the exception: its fundamentals are also
covered in Atlas Sprint 6, Task 6 (ADR-024).

- **No iteration cap** [8 Reliability]. A model stuck retrying `compileJava` would keep paying
  for tokens. I'd add a maximum number of tool calls per request.
- **Unmanaged context growth** [6 Context management]. Covered in the 29K answer above.
- **Silent errors** [5 Failure handling]. `SearchGithubTool` catches `Exception` and returns the message to the model
  without logging it on the server. The same goes for `CompileJavaTool`'s `IOException` path.
  That breaks the project's own rule never to swallow exceptions silently. I'd log them and
  separate retryable errors (429, timeouts) from fatal ones (401).
- **Raw error text reaches the model** [4 Tool security]. The GitHub error result includes the raw response body.
  I'd return a short, cleaned-up message instead.
- **Concurrency assumption** [2 Execution ordering]. `toolsUsed` is a plain `ArrayList`. That's safe only while the
  framework runs tool calls one after another. If it ever runs them concurrently, the list
  needs to be thread-safe.

## API — pending

## Evals — pending

## MCP — pending
