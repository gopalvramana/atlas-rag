package com.atlas.ingestion;

import com.atlas.ingestion.fetcher.DocumentFetcher;
import com.atlas.ingestion.fetcher.FetchedDocument;
import com.atlas.ingestion.parser.DocumentParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(0)
public class FetcherTest implements CommandLineRunner {

    private final DocumentFetcher documentFetcher;
    private final List<DocumentParser> parsers;

    public FetcherTest(DocumentFetcher documentFetcher, List<DocumentParser> parsers) {
        this.documentFetcher = documentFetcher;
        this.parsers = parsers;
    }

    @Override
    public void run(String... args) {
        List<FetchedDocument> documents = documentFetcher.list();

        System.out.println("\n=== Fetcher Test ===");
        System.out.println("Total documents listed: " + documents.size());
        documents.stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.metadata().get("version"),
                        Collectors.counting()))
                .forEach((version, count) ->
                        System.out.printf("  version %-10s → %d files%n", version, count));

        FetchedDocument first = documents.get(0);
        byte[] content = documentFetcher.fetchContent(first);

        DocumentParser parser = parsers.stream()
                .filter(p -> p.supports(first.filename()))
                .findFirst()
                .orElseThrow();

        String plainText = parser.parse(content);

        System.out.printf("%nSample: %s (v%s)%n", first.filename(), first.metadata().get("version"));
        System.out.printf("Raw bytes: %d → Plain text: %d chars%n", content.length, plainText.length());
        System.out.println("First 200 chars: " + plainText.substring(0, Math.min(200, plainText.length())));
    }
}
