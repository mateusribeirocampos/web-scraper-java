package com.campos.webscraper.interfaces.scheduler;

import com.campos.webscraper.application.orchestrator.CrawlJobDispatcher;
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
 * Scheduled trigger that dispatches enabled crawl jobs when they become due.
 */
@Component
public class CrawlJobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlJobScheduler.class);

    private final CrawlJobRepository crawlJobRepository;
    private final CrawlJobDispatcher crawlJobDispatcher;
    private final Clock clock;

    public CrawlJobScheduler(
            CrawlJobRepository crawlJobRepository,
            CrawlJobDispatcher crawlJobDispatcher,
            Clock clock
    ) {
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
        this.crawlJobDispatcher = Objects.requireNonNull(crawlJobDispatcher, "crawlJobDispatcher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Dispatches all enabled jobs that are due at the current instant.
     */
    @Scheduled(cron = "${webscraper.scheduler.crawl-jobs-cron:0 * * * * *}", zone = "UTC")
    public void triggerDueJobs() {
        Instant now = Instant.now(clock);
        List<CrawlJobEntity> dueJobs =
                crawlJobRepository.findByTargetSiteEnabledTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now);

        for (CrawlJobEntity dueJob : dueJobs) {
            try {
                crawlJobDispatcher.dispatch(dueJob);
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to dispatch crawl job id={}", dueJob.getId(), exception);
            }
        }
    }
}
