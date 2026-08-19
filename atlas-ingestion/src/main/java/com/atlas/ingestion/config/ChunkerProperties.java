package com.atlas.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas.chunker")
public record ChunkerProperties(int windowSize, int overlap) {}
