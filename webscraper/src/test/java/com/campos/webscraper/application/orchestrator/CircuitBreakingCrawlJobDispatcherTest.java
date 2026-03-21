package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for circuit breaker and dead-letter routing in dispatch.
 *
 * TDD RED: written before circuit breaker support exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CircuitBreakingCrawlJobDispatcher")
class CircuitBreakingCrawlJobDispatcherTest {

    @Mock
    private CrawlExecutionRepository crawlExecutionRepository;

    @Mock
    private CrawlJobExecutionRunner crawlJobExecutionRunner;

    @Mock
    private DeadLetterQueue deadLetterQueue;

    @Test
    @DisplayName("should record success metrics when dispatch completes successfully")
    void shouldRecordSuccessMetricsWhenDispatchCompletesSuccessfully() {
        Instant now = Instant.parse("2026-03-12T19:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        CrawlJobEntity crawlJob = buildJob(8L, "greenhouse_bitso");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        when(crawlExecutionRepository.save(any(CrawlExecutionEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlExecutionEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        return CrawlExecutionEntity.builder()
                                .id(199L)
                                .crawlJob(entity.getCrawlJob())
                                .status(entity.getStatus())
                                .startedAt(entity.getStartedAt())
                                .finishedAt(entity.getFinishedAt())
                                .pagesVisited(entity.getPagesVisited())
                                .itemsFound(entity.getItemsFound())
                                .retryCount(entity.getRetryCount())
                                .errorMessage(entity.getErrorMessage())
                                .createdAt(entity.getCreatedAt())
                                .build();
                    }
                    return entity;
                });
        when(crawlJobExecutionRunner.run(org.mockito.ArgumentMatchers.eq(crawlJob), any()))
                .thenReturn(new CrawlExecutionOutcome(3, 7));

        CircuitBreakingCrawlJobDispatcher dispatcher = new CircuitBreakingCrawlJobDispatcher(
                crawlExecutionRepository,
                crawlJobExecutionRunner,
                deadLetterQueue,
                CircuitBreakerRegistry.ofDefaults(),
                new CrawlObservabilityService(meterRegistry),
                fixedClock
        );

        assertThat(dispatcher.dispatch(crawlJob)).isEqualTo(CrawlExecutionStatus.SUCCEEDED);
        assertThat(meterRegistry.get("webscraper.crawl.dispatch.total")
                .tag("site_code", "greenhouse_bitso")
                .tag("status", "SUCCEEDED")
                .counter()
                .count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("webscraper.crawl.dispatch.items_found")
                .tag("site_code", "greenhouse_bitso")
                .tag("status", "SUCCEEDED")
                .summary()
                .totalAmount()).isEqualTo(7.0d);
        assertThat(meterRegistry.get("webscraper.crawl.dispatch.duration")
                .tag("site_code", "greenhouse_bitso")
                .tag("status", "SUCCEEDED")
                .timer()
                .count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should route to dead letter and persist DEAD_LETTER when circuit breaker is open")
    void shouldRouteToDeadLetterAndPersistDeadLetterWhenCircuitBreakerIsOpen() {
        Instant now = Instant.parse("2026-03-12T19:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        CrawlJobEntity crawlJob = buildJob(9L);

        when(crawlExecutionRepository.save(any(CrawlExecutionEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlExecutionEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        return CrawlExecutionEntity.builder()
                                .id(200L)
                                .crawlJob(entity.getCrawlJob())
                                .status(entity.getStatus())
                                .startedAt(entity.getStartedAt())
                                .finishedAt(entity.getFinishedAt())
                                .pagesVisited(entity.getPagesVisited())
                                .itemsFound(entity.getItemsFound())
                                .retryCount(entity.getRetryCount())
                                .errorMessage(entity.getErrorMessage())
                                .createdAt(entity.getCreatedAt())
                                .build();
                    }
                    return entity;
                });

        CircuitBreaker circuitBreaker = CircuitBreaker.of("crawlJobRunner", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        circuitBreaker.transitionToOpenState();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("indeed-br", circuitBreaker.getCircuitBreakerConfig()).transitionToOpenState();

        CircuitBreakingCrawlJobDispatcher dispatcher = new CircuitBreakingCrawlJobDispatcher(
                crawlExecutionRepository,
                crawlJobExecutionRunner,
                deadLetterQueue,
                registry,
                new CrawlObservabilityService(new SimpleMeterRegistry()),
                fixedClock
        );

        dispatcher.dispatch(crawlJob);

        ArgumentCaptor<CrawlExecutionEntity> executionCaptor = ArgumentCaptor.forClass(CrawlExecutionEntity.class);
        verify(crawlExecutionRepository, times(2)).save(executionCaptor.capture());
        verify(crawlJobExecutionRunner, never()).run(org.mockito.ArgumentMatchers.eq(crawlJob), any());
        verify(deadLetterQueue).route(crawlJob, "Circuit breaker open for crawl job execution");

        CrawlExecutionEntity finalExecution = executionCaptor.getAllValues().get(1);
        assertThat(finalExecution.getStatus()).isEqualTo(CrawlExecutionStatus.DEAD_LETTER);
        assertThat(finalExecution.getFinishedAt()).isEqualTo(now);
        assertThat(finalExecution.getErrorMessage()).isEqualTo("Circuit breaker open for crawl job execution");
    }

    @Test
    @DisplayName("should keep FAILED status for ordinary runner failures while circuit breaker remains closed")
    void shouldKeepFailedStatusForOrdinaryRunnerFailuresWhileCircuitBreakerRemainsClosed() {
        Instant now = Instant.parse("2026-03-12T19:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        CrawlJobEntity crawlJob = buildJob(10L);

        when(crawlExecutionRepository.save(any(CrawlExecutionEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlExecutionEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        return CrawlExecutionEntity.builder()
                                .id(201L)
                                .crawlJob(entity.getCrawlJob())
                                .status(entity.getStatus())
                                .startedAt(entity.getStartedAt())
                                .finishedAt(entity.getFinishedAt())
                                .pagesVisited(entity.getPagesVisited())
                                .itemsFound(entity.getItemsFound())
                                .retryCount(entity.getRetryCount())
                                .errorMessage(entity.getErrorMessage())
                                .createdAt(entity.getCreatedAt())
                                .build();
                    }
                    return entity;
                });
        when(crawlJobExecutionRunner.run(org.mockito.ArgumentMatchers.eq(crawlJob), any()))
                .thenThrow(new IllegalStateException("temporary upstream error"));

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        CircuitBreakingCrawlJobDispatcher dispatcher = new CircuitBreakingCrawlJobDispatcher(
                crawlExecutionRepository,
                crawlJobExecutionRunner,
                deadLetterQueue,
                registry,
                new CrawlObservabilityService(new SimpleMeterRegistry()),
                fixedClock
        );

        dispatcher.dispatch(crawlJob);

        ArgumentCaptor<CrawlExecutionEntity> executionCaptor = ArgumentCaptor.forClass(CrawlExecutionEntity.class);
        verify(crawlExecutionRepository, times(2)).save(executionCaptor.capture());
        verify(deadLetterQueue, never()).route(any(), any());

        CrawlExecutionEntity finalExecution = executionCaptor.getAllValues().get(1);
        assertThat(finalExecution.getStatus()).isEqualTo(CrawlExecutionStatus.FAILED);
        assertThat(finalExecution.getErrorMessage()).isEqualTo("temporary upstream error");
    }

    @Test
    @DisplayName("should isolate circuit breaker state per target site")
    void shouldIsolateCircuitBreakerStatePerTargetSite() {
        Instant now = Instant.parse("2026-03-12T19:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        CrawlJobEntity failingJob = buildJob(11L, "indeed-br");
        CrawlJobEntity healthyJob = buildJob(12L, "dou-api");

        when(crawlExecutionRepository.save(any(CrawlExecutionEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlExecutionEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        return CrawlExecutionEntity.builder()
                                .id(entity.getCrawlJob().getId() + 1000)
                                .crawlJob(entity.getCrawlJob())
                                .status(entity.getStatus())
                                .startedAt(entity.getStartedAt())
                                .finishedAt(entity.getFinishedAt())
                                .pagesVisited(entity.getPagesVisited())
                                .itemsFound(entity.getItemsFound())
                                .retryCount(entity.getRetryCount())
                                .errorMessage(entity.getErrorMessage())
                                .createdAt(entity.getCreatedAt())
                                .build();
                    }
                    return entity;
                });
        when(crawlJobExecutionRunner.run(org.mockito.ArgumentMatchers.eq(healthyJob), any()))
                .thenReturn(new CrawlExecutionOutcome(3, 7));

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        registry.circuitBreaker("indeed-br").transitionToOpenState();

        CircuitBreakingCrawlJobDispatcher dispatcher = new CircuitBreakingCrawlJobDispatcher(
                crawlExecutionRepository,
                crawlJobExecutionRunner,
                deadLetterQueue,
                registry,
                new CrawlObservabilityService(new SimpleMeterRegistry()),
                fixedClock
        );

        dispatcher.dispatch(failingJob);
        dispatcher.dispatch(healthyJob);

        verify(deadLetterQueue).route(failingJob, "Circuit breaker open for crawl job execution");
        verify(crawlJobExecutionRunner).run(org.mockito.ArgumentMatchers.eq(healthyJob), any());
    }

    private static CrawlJobEntity buildJob(Long id) {
        return buildJob(id, "indeed-br");
    }

    private static CrawlJobEntity buildJob(Long id, String siteCode) {
        Instant now = Instant.parse("2026-03-12T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(id)
                .targetSite(TargetSiteEntity.builder()
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://" + siteCode + ".example.com")
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(now.minusSeconds(600))
                        .build())
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now.minusSeconds(60))
                .build();
    }
}
