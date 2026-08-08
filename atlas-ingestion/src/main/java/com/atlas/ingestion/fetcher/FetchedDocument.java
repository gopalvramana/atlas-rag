package com.atlas.ingestion.fetcher;

import java.util.Map;

public record FetchedDocument(String filename, String path, Map<String, String> metadata) {
}
