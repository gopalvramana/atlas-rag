package com.atlas.ingestion.parser;

public interface DocumentParser {

    boolean supports(String filename);

    String parse(byte[] rawContent);
}
