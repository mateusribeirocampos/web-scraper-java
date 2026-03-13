package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable queue envelope with the minimum materialized metadata needed across async boundaries.
 */
public record EnqueuedCrawlJob(
        Long crawlJobId,
        Long targetSiteId,
        String targetSiteCode,
        ExtractionMode extractionMode,
        JobCategory jobCategory,
        Instant scheduledAt,
        CrawlJobQueueName queueName,
        Instant enqueuedAt
) {

    public EnqueuedCrawlJob {
        Objects.requireNonNull(queueName, "queueName must not be null");
        Objects.requireNonNull(enqueuedAt, "enqueuedAt must not be null");
    }

    public static EnqueuedCrawlJob from(CrawlJobEntity crawlJob, CrawlJobQueueName queueName, Instant enqueuedAt) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(queueName, "queueName must not be null");
        Objects.requireNonNull(enqueuedAt, "enqueuedAt must not be null");

        TargetSiteEntity targetSite = crawlJob.getTargetSite();
        JobCategory effectiveJobCategory = crawlJob.getJobCategory() == null
                ? targetSite == null ? null : targetSite.getJobCategory()
                : crawlJob.getJobCategory();
        return new EnqueuedCrawlJob(
                crawlJob.getId(),
                targetSite == null ? null : targetSite.getId(),
                targetSite == null ? null : targetSite.getSiteCode(),
                targetSite == null ? null : targetSite.getExtractionMode(),
                effectiveJobCategory,
                crawlJob.getScheduledAt(),
                queueName,
                enqueuedAt
        );
    }
}
