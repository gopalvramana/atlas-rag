package com.atlas.ingestion.pipeline;

import com.atlas.core.model.Chunk;
import com.atlas.core.repository.ChunkRepository;
import com.atlas.ingestion.chunker.DocumentChunker;
import com.atlas.ingestion.fetcher.DocumentFetcher;
import com.atlas.ingestion.fetcher.FetchedDocument;
import com.atlas.ingestion.parser.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final DocumentFetcher documentFetcher;
    private final List<DocumentParser> parsers;
    private final DocumentChunker documentChunker;
    private final EmbeddingModel embeddingModel;
    private final ChunkRepository chunkRepository;

    public IngestionPipeline(DocumentFetcher documentFetcher,
                             List<DocumentParser> parsers,
                             DocumentChunker documentChunker,
                             EmbeddingModel embeddingModel,
                             ChunkRepository chunkRepository) {
        this.documentFetcher = documentFetcher;
        this.parsers = parsers;
        this.documentChunker = documentChunker;
        this.embeddingModel = embeddingModel;
        this.chunkRepository = chunkRepository;
    }

    public void run() {
        List<FetchedDocument> documents = documentFetcher.list();
        log.info("Listed {} documents to ingest", documents.size());

        int totalChunksStored = 0;
        int totalChunksSkipped = 0;

        for (FetchedDocument doc : documents) {
            DocumentParser parser = findParser(doc.filename());
            if (parser == null) {
                log.warn("No parser for {}, skipping", doc.filename());
                continue;
            }

            byte[] rawContent = documentFetcher.fetchContent(doc);
            String plainText = parser.parse(rawContent);

            if (plainText.isBlank()) {
                log.warn("Empty content after parsing {}, skipping", doc.filename());
                continue;
            }

            String documentHash = sha256(rawContent);
            String version = doc.metadata().getOrDefault("version", "unknown");
            String sourceFile = doc.path();

            List<String> chunks = documentChunker.chunk(plainText);

            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                String contentHash = sha256(chunkText.getBytes(StandardCharsets.UTF_8));

                if (chunkRepository.existsByContentHashAndVersion(contentHash, version)) {
                    totalChunksSkipped++;
                    continue;
                }

                float[] vector = embeddingModel.embed(chunkText);
                String vectorString = toVectorString(vector);

                Chunk chunk = Chunk.builder()
                        .content(chunkText)
                        .embedding(vectorString)
                        .contentHash(contentHash)
                        .documentHash(documentHash)
                        .sourceFile(sourceFile)
                        .version(version)
                        .chunkIndex(i)
                        .tokenCount(chunkText.split("\\s+").length)
                        .build();

                chunkRepository.save(chunk);
                totalChunksStored++;
            }

            log.info("Processed {} (v{}) → {} chunks", doc.filename(), version, chunks.size());
        }

        log.info("Ingestion complete: {} chunks stored, {} skipped (already exist)",
                totalChunksStored, totalChunksSkipped);
    }

    private DocumentParser findParser(String filename) {
        return parsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElse(null);
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
