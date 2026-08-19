package com.atlas.ingestion.chunker;

import com.atlas.ingestion.config.ChunkerProperties;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SlidingWindowChunker implements DocumentChunker {

    private final int windowSize;
    private final int overlap;
    private final Encoding encoding;

    public SlidingWindowChunker(ChunkerProperties properties) {
        this.windowSize = properties.windowSize();
        this.overlap = properties.overlap();
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    @Override
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        IntArrayList allTokens = encoding.encode(text);
        int totalTokens = allTokens.size();

        if (totalTokens <= windowSize) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int step = windowSize - overlap;

        while (start < totalTokens) {
            int end = Math.min(start + windowSize, totalTokens);

            IntArrayList windowTokens = new IntArrayList();
            for (int i = start; i < end; i++) {
                windowTokens.add(allTokens.get(i));
            }

            String chunkText = encoding.decode(windowTokens);
            chunks.add(chunkText);

            if (end == totalTokens) {
                break;
            }

            start += step;
        }

        return chunks;
    }
}
