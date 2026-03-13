package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Resolves the appropriate queue name for a crawl job based on explicit metadata.
 */
@Component
public class CrawlJobQueueRouter {

    public CrawlJobQueueName route(CrawlJobEntity crawlJob) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");

        ExtractionMode extractionMode = crawlJob.getTargetSite() == null
                ? null
                : crawlJob.getTargetSite().getExtractionMode();
        if (extractionMode == null && effectiveJobCategory(crawlJob) != null) {
            return CrawlJobQueueName.STATIC_SCRAPE_JOBS;
        }
        if (extractionMode == ExtractionMode.API) {
            return CrawlJobQueueName.API_JOBS;
        }
        if (extractionMode == ExtractionMode.BROWSER_AUTOMATION || extractionMode == ExtractionMode.DYNAMIC_HTML) {
            return CrawlJobQueueName.DYNAMIC_BROWSER_JOBS;
        }
        return CrawlJobQueueName.STATIC_SCRAPE_JOBS;
    }

    public CrawlJobQueueName route(EnqueuedCrawlJob crawlJob) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        return crawlJob.queueName();
    }

    private static JobCategory effectiveJobCategory(CrawlJobEntity crawlJob) {
        if (crawlJob.getJobCategory() != null) {
            return crawlJob.getJobCategory();
        }
        return crawlJob.getTargetSite() == null ? null : crawlJob.getTargetSite().getJobCategory();
    }
}
