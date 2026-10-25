ALTER TABLE chunks ADD CONSTRAINT uq_chunks_source_file_chunk_index_version UNIQUE (source_file, chunk_index, version);
