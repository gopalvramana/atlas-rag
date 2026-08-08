CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE chunks (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content        TEXT NOT NULL,
    embedding      vector(1536) NOT NULL,
    content_hash   VARCHAR(64) NOT NULL,
    document_hash  VARCHAR(64) NOT NULL,
    source_file    VARCHAR(512) NOT NULL,
    version        VARCHAR(32) NOT NULL,
    chunk_index    INTEGER NOT NULL,
    token_count    INTEGER NOT NULL,
    created_at     TIMESTAMPTZ DEFAULT now(),

    UNIQUE (content_hash, version)
);

CREATE INDEX idx_chunks_embedding ON chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_chunks_source ON chunks (source_file, version);
