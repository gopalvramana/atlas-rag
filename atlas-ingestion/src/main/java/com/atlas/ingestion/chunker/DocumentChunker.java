package com.atlas.ingestion.chunker;

import java.util.List;

public interface DocumentChunker {

    List<String> chunk(String text);
}
