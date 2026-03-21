package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Dispatches crawl jobs while persisting execution lifecycle and respecting circuit breaker state.
 */
@Component
public class CircuitBreakingCrawlJobDispatcher implements CrawlJobDispatcher {

    private static final String CIRCUIT_OPEN_REASON = "Circuit breaker open for crawl job execution";

    private final CrawlExecutionRepository crawlExecutionRepository;
    private final CrawlJobExecutionRunner crawlJobExecutionRunner;
    private final DeadLetterQueue deadLetterQueue;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CrawlObservabilityService crawlObservabilityService;
    private final Clock clock;

    public CircuitBreakingCrawlJobDispatcher(
            CrawlExecutionRepository crawlExecutionRepository,
            CrawlJobExecutionRunner crawlJobExecutionRunner,
            DeadLetterQueue deadLetterQueue,
            CircuitBreakerRegistry circuitBreakerRegistry,
            CrawlObservabilityService crawlObservabilityService,
            Clock clock
    ) {
        this.crawlExecutionRepository = Objects.requireNonNull(crawlExecutionRepository, "crawlExecutionRepository must not be null");
        this.crawlJobExecutionRunner = Objects.requireNonNull(crawlJobExecutionRunner, "crawlJobExecutionRunner must not be null");
        this.deadLetterQueue = Objects.requireNonNull(deadLetterQueue, "deadLetterQueue must not be null");
        this.circuitBreakerRegistry = Objects.requireNonNull(circuitBreakerRegistry, "circuitBreakerRegistry must not be null");
        this.crawlObservabilityService = Objects.requireNonNull(
                crawlObservabilityService,
                "crawlObservabilityService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public CrawlExecutionStatus dispatch(CrawlJobEntity crawlJob) {
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

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(resolveCircuitBreakerKey(crawlJob));
        Supplier<CrawlExecutionOutcome> protectedRun =
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> crawlJobExecutionRunner.run(crawlJob, runningExecution));

        try {
            CrawlExecutionOutcome outcome = protectedRun.get();
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
            crawlObservabilityService.recordDispatch(
                    crawlJob,
                    CrawlExecutionStatus.SUCCEEDED,
                    outcome.itemsFound(),
                    runningExecution.getStartedAt(),
                    Instant.now(clock),
                    null
            );
            return CrawlExecutionStatus.SUCCEEDED;
        } catch (CallNotPermittedException exception) {
            deadLetterQueue.route(crawlJob, CIRCUIT_OPEN_REASON);
            crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                    .id(runningExecution.getId())
                    .crawlJob(runningExecution.getCrawlJob())
                    .status(CrawlExecutionStatus.DEAD_LETTER)
                    .startedAt(runningExecution.getStartedAt())
                    .finishedAt(Instant.now(clock))
                    .pagesVisited(0)
                    .itemsFound(0)
                    .retryCount(runningExecution.getRetryCount())
                    .errorMessage(CIRCUIT_OPEN_REASON)
                    .createdAt(runningExecution.getCreatedAt())
                    .build());
            crawlObservabilityService.recordDispatch(
                    crawlJob,
                    CrawlExecutionStatus.DEAD_LETTER,
                    0,
                    runningExecution.getStartedAt(),
                    Instant.now(clock),
                    CIRCUIT_OPEN_REASON
            );
            return CrawlExecutionStatus.DEAD_LETTER;
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
            crawlObservabilityService.recordDispatch(
                    crawlJob,
                    CrawlExecutionStatus.FAILED,
                    0,
                    runningExecution.getStartedAt(),
                    Instant.now(clock),
                    exception.getMessage()
            );
            return CrawlExecutionStatus.FAILED;
        }
    }

    private static String resolveCircuitBreakerKey(CrawlJobEntity crawlJob) {
        if (crawlJob.getTargetSite() != null && crawlJob.getTargetSite().getSiteCode() != null) {
            return crawlJob.getTargetSite().getSiteCode();
        }
        if (crawlJob.getJobCategory() != null) {
            return "job-category:" + crawlJob.getJobCategory().name();
        }
        return "default-crawl-job-runner";
    }
}
