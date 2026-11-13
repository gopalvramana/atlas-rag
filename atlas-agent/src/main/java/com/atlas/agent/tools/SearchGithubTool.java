package com.atlas.agent.tools;

import com.atlas.core.model.GitHubResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private final ObjectMapper objectMapper;

    public SearchGithubTool(@Value("${GITHUB_TOKEN}") String token) {
        this.token        = token;
        this.httpClient   = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Tool(description = """
            Search GitHub Issues and Pull Requests in the spring-projects/spring-ai repository.
            Use this when the question is about a known bug, feature request, or change
            that may be tracked as a GitHub issue or PR.
            """)
    public List<GitHubResult> searchGithub(
            @ToolParam(description = "Search terms to look for in issues and PRs") String query) {

        try {
            String encodedQuery = URLEncoder.encode(
                    query + " repo:" + REPO, StandardCharsets.UTF_8);

            String url = "https://api.github.com/search/issues?q=" + encodedQuery + "&per_page=5";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return List.of(new GitHubResult(
                        "GitHub API error", url,
                        "error", "Status " + response.statusCode() + ": " + response.body()));
            }

            JsonNode root  = objectMapper.readTree(response.body());
            JsonNode items = root.get("items");

            List<GitHubResult> results = new ArrayList<>();
            for (JsonNode item : items) {
                String title   = item.get("title").asText();
                String htmlUrl = item.get("html_url").asText();
                String state   = item.get("state").asText();
                String body    = item.has("body") && !item.get("body").isNull()
                        ? item.get("body").asText() : "";
                String excerpt = body.length() > 300 ? body.substring(0, 300) + "..." : body;

                results.add(new GitHubResult(title, htmlUrl, state, excerpt));
            }
            return results;

        } catch (Exception e) {
            return List.of(new GitHubResult(
                    "Error searching GitHub", "", "error", e.getMessage()));
        }
    }

    // --- standalone test ---
    public static void main(String[] args) throws Exception {
        String token = System.getenv("GITHUB_TOKEN");
        SearchGithubTool tool = new SearchGithubTool(token != null ? token : "");

        List<GitHubResult> results = tool.searchGithub("ChatClient builder");
        results.forEach(r -> System.out.printf(
                "[%s] %s%n  %s%n  %s%n%n",
                r.state(), r.title(), r.url(), r.excerpt().replace("\n", " ")));
    }
}
