package com.atlas.agent.tools;

import com.atlas.core.model.GitHubResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.util.List;

/**
 * Searches GitHub Issues and Pull Requests in spring-projects/spring-ai.
 * Uses the same HttpClient + Bearer token pattern as GitHubDocumentFetcher.
 */
@Component
public class SearchGithubTool {

    private static final String REPO = "spring-projects/spring-ai";

    private final String token;
    private final HttpClient httpClient;

    public SearchGithubTool(@Value("${GITHUB_TOKEN}") String token) {
        this.token      = token;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Tool(description = """
            Search GitHub Issues and Pull Requests in the spring-projects/spring-ai repository.
            Use this when the question is about a known bug, feature request, or change
            that may be tracked as a GitHub issue or PR.
            """)
    public List<GitHubResult> searchGithub(
            @ToolParam(description = "Search terms to look for in issues and PRs") String query) {

        // implementation coming next
        throw new UnsupportedOperationException("not yet implemented");
    }
}
