package com.atlas.retrieval;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Minimal Spring Boot entry point for integration tests in atlas-retrieval.
// Production code lives in atlas-api; this class exists only to bootstrap
// the application context when running tests from this module directly.
// EntityScan and EnableJpaRepositories are needed because atlas-core's JPA
// classes live in a different package tree from this module's own classes.
@SpringBootApplication
@EntityScan(basePackages = {"com.atlas.core.model", "com.atlas.retrieval"})
@EnableJpaRepositories(basePackages = {"com.atlas.core.repository", "com.atlas.retrieval"})
public class RetrievalTestApplication {
}
