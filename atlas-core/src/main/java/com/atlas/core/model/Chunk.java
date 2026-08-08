package com.atlas.core.model;

import com.pgvector.PGvector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chunks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"content_hash", "version"})
})
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "vector(1536)")
    private PGvector embedding;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "document_hash", nullable = false, length = 64)
    private String documentHash;

    @Column(name = "source_file", nullable = false, length = 512)
    private String sourceFile;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Chunk() {
    }

    public UUID getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public PGvector getEmbedding() {
        return embedding;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getVersion() {
        return version;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Chunk chunk = new Chunk();

        public Builder content(String content) {
            chunk.content = content;
            return this;
        }

        public Builder embedding(PGvector embedding) {
            chunk.embedding = embedding;
            return this;
        }

        public Builder contentHash(String contentHash) {
            chunk.contentHash = contentHash;
            return this;
        }

        public Builder documentHash(String documentHash) {
            chunk.documentHash = documentHash;
            return this;
        }

        public Builder sourceFile(String sourceFile) {
            chunk.sourceFile = sourceFile;
            return this;
        }

        public Builder version(String version) {
            chunk.version = version;
            return this;
        }

        public Builder chunkIndex(int chunkIndex) {
            chunk.chunkIndex = chunkIndex;
            return this;
        }

        public Builder tokenCount(int tokenCount) {
            chunk.tokenCount = tokenCount;
            return this;
        }

        public Chunk build() {
            return chunk;
        }
    }
}
