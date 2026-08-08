package com.atlas.ingestion.fetcher;

import com.atlas.ingestion.config.GitHubProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GitHubDocumentFetcher implements DocumentFetcher {

    private final GitHubProperties properties;
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubDocumentFetcher(
            GitHubProperties properties,
            @Value("${GITHUB_TOKEN}") String token) {
        this.properties = properties;
        this.token = token;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<FetchedDocument> list() {
        List<FetchedDocument> documents = new ArrayList<>();

        for (String tag : properties.tags()) {
            String version = tag.startsWith("v") ? tag.substring(1) : tag;
            List<String> filePaths = listDocFiles(tag);

            for (String fullPath : filePaths) {
                String relativePath = fullPath.substring(properties.docsPath().length() + 1);
                Map<String, String> metadata = Map.of(
                        "version", version,
                        "source", "github",
                        "repo", properties.owner() + "/" + properties.repo(),
                        "tag", tag
                );
                documents.add(new FetchedDocument(relativePath, fullPath, metadata));
            }
        }

        return documents;
    }

    @Override
    public byte[] fetchContent(FetchedDocument document) {
        String tag = document.metadata().get("tag");
        String url = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                properties.owner(), properties.repo(), document.path(), tag);

        JsonNode response = callGitHubApi(url);
        String base64Content = response.get("content").asText().replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64Content);
    }

    private List<String> listDocFiles(String tag) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1",
                properties.owner(), properties.repo(), tag);

        JsonNode tree = callGitHubApi(url);
        List<String> paths = new ArrayList<>();

        for (JsonNode node : tree.get("tree")) {
            String path = node.get("path").asText();
            if (path.startsWith(properties.docsPath()) && path.endsWith(".adoc")) {
                paths.add(path);
            }
        }

        return paths;
    }

    private JsonNode callGitHubApi(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("GitHub API returned " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GitHub API call interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("GitHub API call failed: " + url, e);
        }
    }
}
