package com.atlas.core.model;

import java.util.List;

public record CompileResult(boolean compiled, List<String> errors) {}
