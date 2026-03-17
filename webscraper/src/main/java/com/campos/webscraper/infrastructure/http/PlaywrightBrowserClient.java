package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;

import java.util.Objects;

/**
 * Abstraction over Playwright to keep the {@link PlaywrightJobFetcher} testable.
 */
public interface PlaywrightBrowserClient {

    /**
     * Performs the browser fetch and returns the raw response that will be mapped to {@code FetchedPage}.
     */
    PlaywrightBrowserResponse fetch(FetchRequest request);

    /**
     * Minimal payload extracted from a Playwright session.
     */
    record PlaywrightBrowserResponse(
            String url,
            String htmlContent,
            int statusCode,
            String contentType
    ) {
        public PlaywrightBrowserResponse {
            Objects.requireNonNull(url, "url must not be null");
        }
    }
}
