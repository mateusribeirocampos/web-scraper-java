package com.campos.webscraper.infrastructure.http;

import java.util.List;

/**
 * Shared contract for public ATS providers that expose published job postings over HTTP.
 *
 * @param <T> transport item returned by the provider
 */
public interface AtsJobProviderClient<T> {

    /**
     * Fetches published jobs from the given provider endpoint.
     */
    List<T> fetchPublishedJobs(String url);
}
