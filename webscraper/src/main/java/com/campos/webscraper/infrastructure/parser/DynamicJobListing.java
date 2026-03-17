package com.campos.webscraper.infrastructure.parser;

import java.util.Objects;

/**
 * Value object representing a job card extracted from a dynamic Playwright page.
 */
public record DynamicJobListing(
        String externalId,
        String title,
        String company,
        String location,
        String url,
        String postedAt,
        boolean remote,
        String description
) {

    public DynamicJobListing {
        Objects.requireNonNull(externalId, "externalId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(company, "company must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(postedAt, "postedAt must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
