package com.atlas.core.model;

import java.util.List;

public record AgentResponse(String answer, List<String> toolsUsed) {}
