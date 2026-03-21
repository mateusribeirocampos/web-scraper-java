package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.application.queue.EnqueuedCrawlJob;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Emits crawl metrics and structured logs for dispatcher and worker lifecycle events.
 */
@Component
public class CrawlObservabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlObservabilityService.class);

    private final MeterRegistry meterRegistry;

    public CrawlObservabilityService(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public void recordDispatch(
            CrawlJobEntity crawlJob,
            CrawlExecutionStatus status,
            int itemsFound,
            Instant startedAt,
            Instant finishedAt,
            String errorMessage
    ) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(finishedAt, "finishedAt must not be null");

        List<Tag> tags = List.of(
                Tag.of("site_code", siteCode(crawlJob)),
                Tag.of("job_category", jobCategory(crawlJob)),
                Tag.of("status", status.name())
        );

        meterRegistry.counter("webscraper.crawl.dispatch.total", tags).increment();
        DistributionSummary.builder("webscraper.crawl.dispatch.items_found")
                .tags(tags)
                .register(meterRegistry)
                .record(itemsFound);
        Timer.builder("webscraper.crawl.dispatch.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.between(startedAt, finishedAt));

        LOGGER.info(
                "event=crawl_dispatch siteCode={} crawlJobId={} jobCategory={} status={} itemsFound={} durationMs={} errorMessage={}",
                siteCode(crawlJob),
                crawlJob.getId(),
                jobCategory(crawlJob),
                status.name(),
                itemsFound,
                Duration.between(startedAt, finishedAt).toMillis(),
                errorMessage == null ? "" : errorMessage
        );
    }

    public void recordWorkerOutcome(CrawlJobQueueName queueName, EnqueuedCrawlJob message, String outcome) {
        Objects.requireNonNull(queueName, "queueName must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");

        String siteCode = message == null || message.targetSiteCode() == null ? "unknown" : message.targetSiteCode();
        List<Tag> tags = List.of(
                Tag.of("queue", queueName.name()),
                Tag.of("outcome", outcome),
                Tag.of("site_code", siteCode)
        );
        meterRegistry.counter("webscraper.crawl.worker.total", tags).increment();

        LOGGER.info(
                "event=crawl_worker queue={} outcome={} crawlJobId={} targetSiteId={} siteCode={} retryCount={}",
                queueName.name(),
                outcome,
                message == null ? null : message.crawlJobId(),
                message == null ? null : message.targetSiteId(),
                siteCode,
                message == null ? null : message.retryCount()
        );
    }

    private static String siteCode(CrawlJobEntity crawlJob) {
        if (crawlJob.getTargetSite() != null && crawlJob.getTargetSite().getSiteCode() != null) {
            return crawlJob.getTargetSite().getSiteCode();
        }
        return "unknown";
    }

    private static String jobCategory(CrawlJobEntity crawlJob) {
        JobCategory effectiveCategory = crawlJob.getJobCategory();
        if (effectiveCategory == null && crawlJob.getTargetSite() != null) {
            effectiveCategory = crawlJob.getTargetSite().getJobCategory();
        }
        return effectiveCategory == null ? "unknown" : effectiveCategory.name();
    }
}
