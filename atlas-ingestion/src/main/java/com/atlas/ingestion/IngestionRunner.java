package com.atlas.ingestion;

import com.atlas.ingestion.pipeline.IngestionPipeline;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class IngestionRunner implements CommandLineRunner {

    private final IngestionPipeline pipeline;

    public IngestionRunner(IngestionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void run(String... args) {
        pipeline.run();
    }
}
