package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;

/**
 * Abstraction for HTTP or browser-backed page retrieval.
 */
public interface JobFetcher {

    /**
     * Fetches a page for the provided request and returns the captured response payload.
     */
    FetchedPage fetch(FetchRequest request);
}
