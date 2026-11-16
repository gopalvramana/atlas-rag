package com.atlas.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

/**
 * Decorator around a ToolCallback that records the tool name into a shared list
 * on every invocation. One instance per tool per request.
 */
class RecordingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final List<String> toolsUsed;

    RecordingToolCallback(ToolCallback delegate, List<String> toolsUsed) {
        this.delegate  = delegate;
        this.toolsUsed = toolsUsed;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        toolsUsed.add(delegate.getToolDefinition().name());
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        toolsUsed.add(delegate.getToolDefinition().name());
        return delegate.call(toolInput, toolContext);
    }
}
