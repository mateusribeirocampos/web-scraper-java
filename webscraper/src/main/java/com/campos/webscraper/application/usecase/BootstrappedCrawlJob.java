package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.domain.model.CrawlJobEntity;

import java.util.Objects;

public record BootstrappedCrawlJob(
        BootstrapStatus bootstrapStatus,
        CrawlJobEntity crawlJob
) {

    public BootstrappedCrawlJob {
        Objects.requireNonNull(bootstrapStatus, "bootstrapStatus must not be null");
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
    }
}
