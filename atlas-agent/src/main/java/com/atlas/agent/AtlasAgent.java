package com.atlas.agent;

import com.atlas.agent.tools.CompileJavaTool;
import com.atlas.agent.tools.SearchDocsTool;
import com.atlas.agent.tools.SearchGithubTool;
import com.atlas.core.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the ReAct loop: model reasons over the question, calls tools as needed,
 * and produces a final answer. Spring AI manages the loop automatically —
 * we register the tools and the model decides which to call and when.
 */
@Service
public class AtlasAgent {

    private static final Logger log = LoggerFactory.getLogger(AtlasAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert Spring AI assistant.

            You have access to three tools:
            - searchDocs: use this for questions about Spring AI concepts, APIs, and configuration.
            - compileJava: use this to verify that a Java code example compiles correctly.
            - searchGithub: use this for questions about known bugs, issues, or feature requests.

            Always use the most relevant tool before answering.
            Cite the source for every factual claim.
            """;

    private final ChatClient chatClient;
    private final SearchDocsTool searchDocsTool;
    private final CompileJavaTool compileJavaTool;
    private final SearchGithubTool searchGithubTool;

    public AtlasAgent(
            ChatClient.Builder chatClientBuilder,
            SearchDocsTool searchDocsTool,
            CompileJavaTool compileJavaTool,
            SearchGithubTool searchGithubTool) {
        this.searchDocsTool  = searchDocsTool;
        this.compileJavaTool = compileJavaTool;
        this.searchGithubTool = searchGithubTool;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    public AgentResponse ask(String question) {
        log.info("AtlasAgent: question='{}'", question);

        List<String> toolsUsed = new ArrayList<>();

        ToolCallback[] wrapped = wrap(
                ToolCallbacks.from(searchDocsTool, compileJavaTool, searchGithubTool),
                toolsUsed);

        String answer = chatClient.prompt()
                .user(question)
                .toolCallbacks(wrapped)
                .call()
                .content();

        log.info("AtlasAgent: answer generated, toolsUsed={}", toolsUsed);
        return new AgentResponse(answer, List.copyOf(toolsUsed));
    }

    private static ToolCallback[] wrap(ToolCallback[] callbacks, List<String> toolsUsed) {
        ToolCallback[] wrapped = new ToolCallback[callbacks.length];
        for (int i = 0; i < callbacks.length; i++) {
            wrapped[i] = new RecordingToolCallback(callbacks[i], toolsUsed);
        }
        return wrapped;
    }
}
