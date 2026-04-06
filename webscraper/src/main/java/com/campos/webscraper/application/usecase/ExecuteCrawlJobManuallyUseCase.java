package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.orchestrator.CrawlJobDispatcher;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.shared.CrawlJobNotFoundException;
import com.campos.webscraper.shared.TargetSiteActivationBlockedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Manual trigger use case that dispatches an existing crawl job by id.
 */
@Component
public class ExecuteCrawlJobManuallyUseCase {

    private final CrawlJobRepository crawlJobRepository;
    private final CrawlJobDispatcher crawlJobDispatcher;

    public ExecuteCrawlJobManuallyUseCase(
            CrawlJobRepository crawlJobRepository,
            CrawlJobDispatcher crawlJobDispatcher
    ) {
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
        this.crawlJobDispatcher = Objects.requireNonNull(crawlJobDispatcher, "crawlJobDispatcher must not be null");
    }

    /**
     * Loads the crawl job and dispatches it immediately.
     */
    public void execute(Long jobId) {
        CrawlJobEntity crawlJob = crawlJobRepository.findById(jobId)
                .orElseThrow(() -> new CrawlJobNotFoundException(jobId));
        assertTargetSiteRunnable(crawlJob);

        crawlJobDispatcher.dispatch(crawlJob);
    }

    private static void assertTargetSiteRunnable(CrawlJobEntity crawlJob) {
        if (crawlJob.getTargetSite().getLegalStatus() == LegalStatus.SCRAPING_PROIBIDO) {
            throw new TargetSiteActivationBlockedException(
                    crawlJob.getTargetSite().getId(),
                    List.of("target site is blocked by compliance")
            );
        }
    }
}
