package com.atlas.ingestion;

import com.atlas.ingestion.config.ChunkerProperties;
import com.atlas.ingestion.config.GitHubProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.atlas.core.model")
@EnableJpaRepositories(basePackages = "com.atlas.core.repository")
@EnableConfigurationProperties({GitHubProperties.class, ChunkerProperties.class})
public class IngestionApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(IngestionApplication.class, args);
    }
}
