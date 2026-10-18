package com.atlas.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.atlas.api", "com.atlas.retrieval"})
@EntityScan(basePackages = "com.atlas.core.model")
@EnableJpaRepositories(basePackages = "com.atlas.core.repository")
public class ApiApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .ignoreIfMissing()
                .load();

        // Fallback: look in parent directory (when running from module dir via mvn spring-boot:run)
        if (dotenv.get("DB_URL", "").isBlank()) {
            dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir") + "/..")
                    .ignoreIfMissing()
                    .load();
        }

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(ApiApplication.class, args);
    }
}
