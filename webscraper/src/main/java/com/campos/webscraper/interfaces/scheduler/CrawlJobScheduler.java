package com.campos.webscraper.interfaces.scheduler;

import com.campos.webscraper.application.queue.CrawlJobQueue;
import com.campos.webscraper.application.queue.CrawlJobQueueRouter;
import com.campos.webscraper.application.queue.InFlightCrawlJobRegistry;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Scheduled trigger that enqueues enabled crawl jobs when they become due.
 */
@Component
public class CrawlJobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlJobScheduler.class);

    private final CrawlJobRepository crawlJobRepository;
    private final CrawlJobQueue crawlJobQueue;
    private final CrawlJobQueueRouter crawlJobQueueRouter;
    private final InFlightCrawlJobRegistry inFlightCrawlJobRegistry;
    private final Clock clock;

    public CrawlJobScheduler(
            CrawlJobRepository crawlJobRepository,
            CrawlJobQueue crawlJobQueue,
            CrawlJobQueueRouter crawlJobQueueRouter,
            InFlightCrawlJobRegistry inFlightCrawlJobRegistry,
            Clock clock
    ) {
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
        this.crawlJobQueue = Objects.requireNonNull(crawlJobQueue, "crawlJobQueue must not be null");
        this.crawlJobQueueRouter = Objects.requireNonNull(crawlJobQueueRouter, "crawlJobQueueRouter must not be null");
        this.inFlightCrawlJobRegistry = Objects.requireNonNull(
                inFlightCrawlJobRegistry,
                "inFlightCrawlJobRegistry must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Dispatches all enabled jobs that are due at the current instant.
     */
    @Scheduled(cron = "${webscraper.scheduler.crawl-jobs-cron:0 * * * * *}", zone = "UTC")
    public void triggerDueJobs() {
        Instant now = Instant.now(clock);
        List<CrawlJobEntity> dueJobs =
                crawlJobRepository.findByTargetSiteEnabledTrueAndSchedulerManagedTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now);

        for (CrawlJobEntity dueJob : dueJobs) {
            try {
                if (dueJob.getId() != null && !inFlightCrawlJobRegistry.tryClaim(dueJob.getId())) {
                    continue;
                }
                crawlJobQueue.enqueue(dueJob, crawlJobQueueRouter.route(dueJob));
            } catch (RuntimeException exception) {
                inFlightCrawlJobRegistry.release(dueJob.getId());
                LOGGER.error("Failed to enqueue crawl job id={}", dueJob.getId(), exception);
            }
        }
    }
}
