package com.atlas.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "atlas.github")
public record GitHubProperties(
        String owner,
        String repo,
        String docsPath,
        List<String> tags
) {
}
