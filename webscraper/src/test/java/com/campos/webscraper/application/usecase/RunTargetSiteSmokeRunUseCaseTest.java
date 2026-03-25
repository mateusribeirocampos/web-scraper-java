package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.orchestrator.CrawlJobDispatcher;
import com.campos.webscraper.application.queue.InFlightCrawlJobRegistry;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("RunTargetSiteSmokeRunUseCase")
class RunTargetSiteSmokeRunUseCaseTest {

    @Mock
    private BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase;

    @Mock
    private CrawlJobDispatcher crawlJobDispatcher;

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Mock
    private InFlightCrawlJobRegistry inFlightCrawlJobRegistry;

    @Test
    @DisplayName("should bootstrap crawl job and dispatch smoke run for target site")
    void shouldBootstrapCrawlJobAndDispatchSmokeRunForTargetSite() {
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(101L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:01:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();

        when(bootstrapCrawlJobFromTargetSiteUseCase.executeForSmokeRun(anyLong(), any(Instant.class)))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.CREATED, crawlJob));
        when(inFlightCrawlJobRegistry.tryClaim(101L)).thenReturn(true);
        when(crawlJobRepository.save(any(CrawlJobEntity.class))).thenAnswer(invocation -> {
            CrawlJobEntity candidate = invocation.getArgument(0);
            return CrawlJobEntity.builder()
                    .id(202L)
                    .targetSite(candidate.getTargetSite())
                    .scheduledAt(candidate.getScheduledAt())
                    .jobCategory(candidate.getJobCategory())
                    .schedulerManaged(candidate.isSchedulerManaged())
                    .createdAt(candidate.getCreatedAt())
                    .build();
        });
        when(crawlJobDispatcher.dispatch(any(CrawlJobEntity.class))).thenReturn(CrawlExecutionStatus.SUCCEEDED);

        RunTargetSiteSmokeRunUseCase useCase = new RunTargetSiteSmokeRunUseCase(
                bootstrapCrawlJobFromTargetSiteUseCase,
                crawlJobDispatcher,
                crawlJobRepository,
                inFlightCrawlJobRegistry,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        TargetSiteSmokeRunResult result = useCase.execute(7L);

        assertThat(result.siteId()).isEqualTo(7L);
        assertThat(result.siteCode()).isEqualTo("greenhouse_bitso");
        assertThat(result.jobId()).isEqualTo(202L);
        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.CREATED);
        assertThat(result.smokeRunStatus()).isEqualTo("DISPATCHED");
        assertThat(result.dispatchStatus()).isEqualTo(CrawlExecutionStatus.SUCCEEDED);
        ArgumentCaptor<CrawlJobEntity> captor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(inFlightCrawlJobRegistry).tryClaim(101L);
        verify(inFlightCrawlJobRegistry).release(101L);
        verify(crawlJobRepository).save(captor.capture());
        assertThat(captor.getValue().getScheduledAt()).isEqualTo(Instant.parse("2026-03-24T20:00:00Z"));
        assertThat(captor.getValue().isSchedulerManaged()).isFalse();
        assertThat(captor.getValue().getId()).isNull();
        verify(bootstrapCrawlJobFromTargetSiteUseCase).executeForSmokeRun(anyLong(), any(Instant.class));
        verify(crawlJobDispatcher).dispatch(any(CrawlJobEntity.class));
    }

    @Test
    @DisplayName("should not reschedule non scheduler-managed crawl job during smoke run")
    void shouldNotRescheduleNonSchedulerManagedCrawlJobDuringSmokeRun() {
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(101L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:00:00Z"))
                .schedulerManaged(false)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();

        when(bootstrapCrawlJobFromTargetSiteUseCase.executeForSmokeRun(anyLong(), any(Instant.class)))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.UPDATED, crawlJob));
        when(crawlJobDispatcher.dispatch(crawlJob)).thenReturn(CrawlExecutionStatus.SUCCEEDED);

        RunTargetSiteSmokeRunUseCase useCase = new RunTargetSiteSmokeRunUseCase(
                bootstrapCrawlJobFromTargetSiteUseCase,
                crawlJobDispatcher,
                crawlJobRepository,
                inFlightCrawlJobRegistry,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        TargetSiteSmokeRunResult result = useCase.execute(7L);

        assertThat(result.smokeRunStatus()).isEqualTo("DISPATCHED");
        assertThat(result.dispatchStatus()).isEqualTo(CrawlExecutionStatus.SUCCEEDED);
        verify(inFlightCrawlJobRegistry, never()).tryClaim(any());
        verify(bootstrapCrawlJobFromTargetSiteUseCase).executeForSmokeRun(anyLong(), any(Instant.class));
        verify(crawlJobDispatcher).dispatch(crawlJob);
        verify(crawlJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("should skip smoke run when canonical crawl job is already in flight")
    void shouldSkipSmokeRunWhenCanonicalCrawlJobIsAlreadyInFlight() {
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(101L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:00:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();

        when(bootstrapCrawlJobFromTargetSiteUseCase.executeForSmokeRun(anyLong(), any(Instant.class)))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.UPDATED, crawlJob));
        when(inFlightCrawlJobRegistry.tryClaim(101L)).thenReturn(false);

        RunTargetSiteSmokeRunUseCase useCase = new RunTargetSiteSmokeRunUseCase(
                bootstrapCrawlJobFromTargetSiteUseCase,
                crawlJobDispatcher,
                crawlJobRepository,
                inFlightCrawlJobRegistry,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        TargetSiteSmokeRunResult result = useCase.execute(7L);

        assertThat(result.smokeRunStatus()).isEqualTo("SKIPPED_IN_FLIGHT");
        assertThat(result.dispatchStatus()).isNull();
        verify(inFlightCrawlJobRegistry).tryClaim(101L);
        verify(crawlJobDispatcher, never()).dispatch(any());
        verify(crawlJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("should advance canonical schedule and still dispatch smoke run when canonical crawl job is due")
    void shouldAdvanceCanonicalScheduleAndStillDispatchSmokeRunWhenCanonicalCrawlJobIsDue() {
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(101L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T19:59:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T19:00:00Z"))
                .build();

        when(bootstrapCrawlJobFromTargetSiteUseCase.executeForSmokeRun(anyLong(), any(Instant.class)))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.UPDATED, crawlJob));
        when(inFlightCrawlJobRegistry.tryClaim(101L)).thenReturn(true);
        when(crawlJobRepository.save(any(CrawlJobEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlJobEntity candidate = invocation.getArgument(0);
                    if (candidate.isSchedulerManaged()) {
                        return candidate;
                    }
                    return CrawlJobEntity.builder()
                            .id(202L)
                            .targetSite(candidate.getTargetSite())
                            .scheduledAt(candidate.getScheduledAt())
                            .jobCategory(candidate.getJobCategory())
                            .schedulerManaged(candidate.isSchedulerManaged())
                            .createdAt(candidate.getCreatedAt())
                            .build();
                });
        when(crawlJobDispatcher.dispatch(any(CrawlJobEntity.class))).thenReturn(CrawlExecutionStatus.SUCCEEDED);

        RunTargetSiteSmokeRunUseCase useCase = new RunTargetSiteSmokeRunUseCase(
                bootstrapCrawlJobFromTargetSiteUseCase,
                crawlJobDispatcher,
                crawlJobRepository,
                inFlightCrawlJobRegistry,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        TargetSiteSmokeRunResult result = useCase.execute(7L);

        assertThat(result.smokeRunStatus()).isEqualTo("DISPATCHED");
        assertThat(result.dispatchStatus()).isEqualTo(CrawlExecutionStatus.SUCCEEDED);
        assertThat(result.jobId()).isEqualTo(202L);
        ArgumentCaptor<CrawlJobEntity> captor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(crawlJobRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getId()).isNull();
        assertThat(captor.getAllValues().get(0).isSchedulerManaged()).isFalse();
        assertThat(captor.getAllValues().get(1).getId()).isEqualTo(101L);
        assertThat(captor.getAllValues().get(1).getScheduledAt()).isEqualTo(Instant.parse("2026-03-24T20:01:00Z"));
        assertThat(captor.getAllValues().get(1).isSchedulerManaged()).isTrue();
        verify(inFlightCrawlJobRegistry).tryClaim(101L);
        verify(inFlightCrawlJobRegistry).release(101L);
        verify(crawlJobDispatcher).dispatch(any(CrawlJobEntity.class));
    }

    @Test
    @DisplayName("should advance near-future canonical schedule to smoke run floor before dispatch")
    void shouldAdvanceNearFutureCanonicalScheduleToSmokeRunFloorBeforeDispatch() {
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(101L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:00:20Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T19:00:00Z"))
                .build();

        when(bootstrapCrawlJobFromTargetSiteUseCase.executeForSmokeRun(anyLong(), any(Instant.class)))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.UPDATED, crawlJob));
        when(inFlightCrawlJobRegistry.tryClaim(101L)).thenReturn(true);
        when(crawlJobRepository.save(any(CrawlJobEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlJobEntity candidate = invocation.getArgument(0);
                    if (candidate.isSchedulerManaged()) {
                        return candidate;
                    }
                    return CrawlJobEntity.builder()
                            .id(202L)
                            .targetSite(candidate.getTargetSite())
                            .scheduledAt(candidate.getScheduledAt())
                            .jobCategory(candidate.getJobCategory())
                            .schedulerManaged(candidate.isSchedulerManaged())
                            .createdAt(candidate.getCreatedAt())
                            .build();
                });
        when(crawlJobDispatcher.dispatch(any(CrawlJobEntity.class))).thenReturn(CrawlExecutionStatus.SUCCEEDED);

        RunTargetSiteSmokeRunUseCase useCase = new RunTargetSiteSmokeRunUseCase(
                bootstrapCrawlJobFromTargetSiteUseCase,
                crawlJobDispatcher,
                crawlJobRepository,
                inFlightCrawlJobRegistry,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        TargetSiteSmokeRunResult result = useCase.execute(7L);

        assertThat(result.smokeRunStatus()).isEqualTo("DISPATCHED");
        assertThat(result.dispatchStatus()).isEqualTo(CrawlExecutionStatus.SUCCEEDED);
        assertThat(result.jobId()).isEqualTo(202L);
        ArgumentCaptor<CrawlJobEntity> captor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(crawlJobRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getId()).isNull();
        assertThat(captor.getAllValues().get(0).isSchedulerManaged()).isFalse();
        assertThat(captor.getAllValues().get(1).getId()).isEqualTo(101L);
        assertThat(captor.getAllValues().get(1).getScheduledAt()).isEqualTo(Instant.parse("2026-03-24T20:01:00Z"));
        assertThat(captor.getAllValues().get(1).isSchedulerManaged()).isTrue();
        verify(inFlightCrawlJobRegistry).tryClaim(101L);
        verify(inFlightCrawlJobRegistry).release(101L);
        verify(crawlJobDispatcher).dispatch(any(CrawlJobEntity.class));
    }

    @Test
    @DisplayName("should release in-flight claim when transient smoke-run job save fails before dispatch")
    void shouldReleaseInFlightClaimWhenTransientSmokeRunJobSaveFailsBeforeDispatch() {
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(101L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:00:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();

        when(bootstrapCrawlJobFromTargetSiteUseCase.executeForSmokeRun(anyLong(), any(Instant.class)))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.UPDATED, crawlJob));
        when(inFlightCrawlJobRegistry.tryClaim(101L)).thenReturn(true);
        when(crawlJobRepository.save(any(CrawlJobEntity.class))).thenThrow(new RuntimeException("save failed"));

        RunTargetSiteSmokeRunUseCase useCase = new RunTargetSiteSmokeRunUseCase(
                bootstrapCrawlJobFromTargetSiteUseCase,
                crawlJobDispatcher,
                crawlJobRepository,
                inFlightCrawlJobRegistry,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> useCase.execute(7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("save failed");

        verify(inFlightCrawlJobRegistry).tryClaim(101L);
        verify(inFlightCrawlJobRegistry).release(101L);
        verify(crawlJobDispatcher, never()).dispatch(any());
    }
}
