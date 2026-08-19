package com.atlas.ingestion;

import com.atlas.ingestion.chunker.DocumentChunker;
import com.atlas.ingestion.fetcher.DocumentFetcher;
import com.atlas.ingestion.fetcher.FetchedDocument;
import com.atlas.ingestion.parser.DocumentParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

// @Component — disabled; use IngestionRunner instead
public class ChunkerTest implements CommandLineRunner {

    private final DocumentFetcher documentFetcher;
    private final List<DocumentParser> parsers;
    private final DocumentChunker documentChunker;

    public ChunkerTest(DocumentFetcher documentFetcher,
                       List<DocumentParser> parsers,
                       DocumentChunker documentChunker) {
        this.documentFetcher = documentFetcher;
        this.parsers = parsers;
        this.documentChunker = documentChunker;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=== Chunker Test ===");

        List<FetchedDocument> documents = documentFetcher.list();
        FetchedDocument doc = documents.get(0);

        byte[] content = documentFetcher.fetchContent(doc);
        DocumentParser parser = parsers.stream()
                .filter(p -> p.supports(doc.filename()))
                .findFirst()
                .orElseThrow();

        String plainText = parser.parse(content);
        List<String> chunks = documentChunker.chunk(plainText);

        System.out.printf("Document: %s (v%s)%n", doc.filename(), doc.metadata().get("version"));
        System.out.printf("Plain text: %d chars%n", plainText.length());
        System.out.printf("Chunks: %d%n", chunks.size());

        for (int i = 0; i < Math.min(3, chunks.size()); i++) {
            System.out.printf("%nChunk %d (%d chars):%n", i, chunks.get(i).length());
            System.out.println(chunks.get(i).substring(0, Math.min(150, chunks.get(i).length())) + "...");
        }

        if (chunks.size() > 1) {
            String end1 = chunks.get(0).substring(Math.max(0, chunks.get(0).length() - 80));
            String start2 = chunks.get(1).substring(0, Math.min(80, chunks.get(1).length()));
            System.out.println("\n--- Overlap check ---");
            System.out.println("End of chunk 0:   ..." + end1);
            System.out.println("Start of chunk 1: " + start2 + "...");
        }
    }
}
