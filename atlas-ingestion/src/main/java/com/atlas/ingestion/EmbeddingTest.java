package com.atlas.ingestion;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// @Component — disabled; use IngestionRunner instead
public class EmbeddingTest implements CommandLineRunner {

    private final EmbeddingModel embeddingModel;

    public EmbeddingTest(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        String sampleText = "Spring AI provides an embedding model interface for text vectorization";

        float[] embedding = embeddingModel.embed(sampleText);

        System.out.println("Input: " + sampleText);
        System.out.println("Dimensions: " + embedding.length);
        System.out.printf("First 5 values: [%.6f, %.6f, %.6f, %.6f, %.6f]%n",
                embedding[0], embedding[1], embedding[2], embedding[3], embedding[4]);
        System.out.println("==>"+ Arrays.toString(embedding));
    }
}
