-- Add full-text search vector column for keyword search (PostgreSQL ts_rank, not BM25)
ALTER TABLE chunks ADD COLUMN search_vector tsvector;

-- GIN index for fast full-text search
CREATE INDEX idx_chunks_search_vector ON chunks USING GIN(search_vector);

-- Populate for existing rows
UPDATE chunks SET search_vector = to_tsvector('english', content);

-- Trigger to keep search_vector current on insert and update
CREATE OR REPLACE FUNCTION chunks_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', NEW.content);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER chunks_search_vector_trigger
    BEFORE INSERT OR UPDATE OF content ON chunks
    FOR EACH ROW EXECUTE FUNCTION chunks_search_vector_update();
