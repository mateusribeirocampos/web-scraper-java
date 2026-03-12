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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the dispatcher that persists crawl execution lifecycle.
 *
 * TDD RED: written before the persistent dispatcher exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("PersistentCrawlJobDispatcher")
class PersistentCrawlJobDispatcherTest {

    @Mock
    private CrawlExecutionRepository crawlExecutionRepository;

    @Mock
    private CrawlJobExecutionRunner crawlJobExecutionRunner;

    @Test
    @DisplayName("should persist running and succeeded execution with counters")
    void shouldPersistRunningAndSucceededExecutionWithCounters() {
        Instant now = Instant.parse("2026-03-12T18:30:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        CrawlJobEntity crawlJob = buildJob(7L);

        when(crawlExecutionRepository.save(any(CrawlExecutionEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlExecutionEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        return CrawlExecutionEntity.builder()
                                .id(100L)
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
        when(crawlJobExecutionRunner.run(crawlJob)).thenReturn(new CrawlExecutionOutcome(5, 12));

        PersistentCrawlJobDispatcher dispatcher =
                new PersistentCrawlJobDispatcher(crawlExecutionRepository, crawlJobExecutionRunner, fixedClock);

        dispatcher.dispatch(crawlJob);

        ArgumentCaptor<CrawlExecutionEntity> executionCaptor = ArgumentCaptor.forClass(CrawlExecutionEntity.class);
        verify(crawlExecutionRepository, times(2)).save(executionCaptor.capture());

        CrawlExecutionEntity runningExecution = executionCaptor.getAllValues().get(0);
        CrawlExecutionEntity finishedExecution = executionCaptor.getAllValues().get(1);

        assertThat(runningExecution.getStatus()).isEqualTo(CrawlExecutionStatus.RUNNING);
        assertThat(runningExecution.getStartedAt()).isEqualTo(now);
        assertThat(runningExecution.getFinishedAt()).isNull();

        assertThat(finishedExecution.getId()).isEqualTo(100L);
        assertThat(finishedExecution.getStatus()).isEqualTo(CrawlExecutionStatus.SUCCEEDED);
        assertThat(finishedExecution.getStartedAt()).isEqualTo(now);
        assertThat(finishedExecution.getFinishedAt()).isEqualTo(now);
        assertThat(finishedExecution.getPagesVisited()).isEqualTo(5);
        assertThat(finishedExecution.getItemsFound()).isEqualTo(12);
        assertThat(finishedExecution.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("should persist failed execution with error message when runner throws")
    void shouldPersistFailedExecutionWithErrorMessageWhenRunnerThrows() {
        Instant now = Instant.parse("2026-03-12T18:30:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        CrawlJobEntity crawlJob = buildJob(8L);

        when(crawlExecutionRepository.save(any(CrawlExecutionEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlExecutionEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        return CrawlExecutionEntity.builder()
                                .id(101L)
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
        when(crawlJobExecutionRunner.run(crawlJob)).thenThrow(new IllegalStateException("boom"));

        PersistentCrawlJobDispatcher dispatcher =
                new PersistentCrawlJobDispatcher(crawlExecutionRepository, crawlJobExecutionRunner, fixedClock);

        dispatcher.dispatch(crawlJob);

        ArgumentCaptor<CrawlExecutionEntity> executionCaptor = ArgumentCaptor.forClass(CrawlExecutionEntity.class);
        verify(crawlExecutionRepository, times(2)).save(executionCaptor.capture());

        CrawlExecutionEntity finishedExecution = executionCaptor.getAllValues().get(1);

        assertThat(finishedExecution.getId()).isEqualTo(101L);
        assertThat(finishedExecution.getStatus()).isEqualTo(CrawlExecutionStatus.FAILED);
        assertThat(finishedExecution.getFinishedAt()).isEqualTo(now);
        assertThat(finishedExecution.getPagesVisited()).isEqualTo(0);
        assertThat(finishedExecution.getItemsFound()).isEqualTo(0);
        assertThat(finishedExecution.getErrorMessage()).isEqualTo("boom");
    }

    private static CrawlJobEntity buildJob(Long id) {
        Instant now = Instant.parse("2026-03-12T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(id)
                .targetSite(TargetSiteEntity.builder()
                        .siteCode("indeed-br")
                        .displayName("Indeed Brasil")
                        .baseUrl("https://br.indeed.com")
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
