package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Dispatches crawl jobs while persisting their execution lifecycle and counters.
 */
@Component
public class PersistentCrawlJobDispatcher implements CrawlJobDispatcher {

    private final CrawlExecutionRepository crawlExecutionRepository;
    private final CrawlJobExecutionRunner crawlJobExecutionRunner;
    private final Clock clock;

    public PersistentCrawlJobDispatcher(
            CrawlExecutionRepository crawlExecutionRepository,
            CrawlJobExecutionRunner crawlJobExecutionRunner,
            Clock clock
    ) {
        this.crawlExecutionRepository = Objects.requireNonNull(
                crawlExecutionRepository,
                "crawlExecutionRepository must not be null"
        );
        this.crawlJobExecutionRunner = Objects.requireNonNull(
                crawlJobExecutionRunner,
                "crawlJobExecutionRunner must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void dispatch(CrawlJobEntity crawlJob) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");

        Instant now = Instant.now(clock);
        CrawlExecutionEntity runningExecution = crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                .crawlJob(crawlJob)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(now)
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(now)
                .build());

        try {
            CrawlExecutionOutcome outcome = crawlJobExecutionRunner.run(crawlJob);
            crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                    .id(runningExecution.getId())
                    .crawlJob(runningExecution.getCrawlJob())
                    .status(CrawlExecutionStatus.SUCCEEDED)
                    .startedAt(runningExecution.getStartedAt())
                    .finishedAt(Instant.now(clock))
                    .pagesVisited(outcome.pagesVisited())
                    .itemsFound(outcome.itemsFound())
                    .retryCount(runningExecution.getRetryCount())
                    .errorMessage(null)
                    .createdAt(runningExecution.getCreatedAt())
                    .build());
        } catch (RuntimeException exception) {
            crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                    .id(runningExecution.getId())
                    .crawlJob(runningExecution.getCrawlJob())
                    .status(CrawlExecutionStatus.FAILED)
                    .startedAt(runningExecution.getStartedAt())
                    .finishedAt(Instant.now(clock))
                    .pagesVisited(0)
                    .itemsFound(0)
                    .retryCount(runningExecution.getRetryCount())
                    .errorMessage(exception.getMessage())
                    .createdAt(runningExecution.getCreatedAt())
                    .build());
        }
    }
}
