package com.atlas.retrieval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.Executors;

@Configuration
class RetrievalConfig {

    // Limits concurrent DB searches — prevents connection pool exhaustion under load.
    // Each hybrid search issues 2 concurrent queries (vector + keyword).
    // With concurrency=10, at most 20 simultaneous DB connections are used for search.
    @Bean(destroyMethod = "shutdown")
    ExecutorService searchExecutor(
            @Value("${atlas.retrieval.search-concurrency:10}") int concurrency) {
        // Bounded semaphore on top of virtual threads: virtual threads handle I/O cheaply,
        // the semaphore controls admission so we never saturate the connection pool.
        Semaphore semaphore = new Semaphore(concurrency);
        ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();

        return new BoundedVirtualExecutor(delegate, semaphore);
    }
}
