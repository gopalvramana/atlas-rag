package com.atlas.ingestion;

import com.atlas.ingestion.parser.DocumentParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
@Order(1)
public class ParserTest implements CommandLineRunner {

    private final List<DocumentParser> parsers;

    public ParserTest(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    @Override
    public void run(String... args) throws Exception {
        String url = "https://raw.githubusercontent.com/spring-projects/spring-ai/main/"
                + "spring-ai-docs/src/main/antora/modules/ROOT/pages/api/chatmodel.adoc";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        byte[] fileBytes = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();

        String filename = "chatmodel.adoc";

        DocumentParser parser = parsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No parser for: " + filename));

        String plainText = parser.parse(fileBytes);

        System.out.println("\n=== Parser Test ===");
        System.out.println("File: " + filename);
        System.out.println("Raw bytes: " + fileBytes.length);
        System.out.println("Plain text length: " + plainText.length());
        //System.out.println("First 500 chars:\n" + plainText.substring(0, Math.min(500, plainText.length())));
        System.out.println("First 500 chars:\n" + plainText);
    }
}
