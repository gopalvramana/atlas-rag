package com.atlas.ingestion.parser;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AsciiDocParser implements DocumentParser {

    private final Asciidoctor asciidoctor;

    public AsciiDocParser() {
        this.asciidoctor = Asciidoctor.Factory.create();
    }

    @Override
    public boolean supports(String filename) {
        return filename.endsWith(".adoc");
    }

    @Override
    public String parse(byte[] rawContent) {
        String adocContent = new String(rawContent, StandardCharsets.UTF_8);
        String html = asciidoctor.convert(adocContent, Options.builder().build());
        return Jsoup.parse(html).text();
    }
}
