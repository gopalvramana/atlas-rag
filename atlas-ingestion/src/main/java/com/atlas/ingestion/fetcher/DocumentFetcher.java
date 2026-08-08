package com.atlas.ingestion.fetcher;

import java.util.List;

public interface DocumentFetcher {

    List<FetchedDocument> list();

    byte[] fetchContent(FetchedDocument document);
}
