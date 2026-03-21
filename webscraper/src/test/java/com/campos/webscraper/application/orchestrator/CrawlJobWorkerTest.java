package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.application.queue.CrawlJobQueue;
import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.application.queue.EnqueuedCrawlJob;
import com.campos.webscraper.application.queue.InFlightCrawlJobRegistry;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlJobWorker")
class CrawlJobWorkerTest {

    @Mock
    private CrawlJobQueue crawlJobQueue;

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Mock
    private TargetSiteRepository targetSiteRepository;

    @Mock
    private CrawlJobDispatcher crawlJobDispatcher;

    @Mock
    private InFlightCrawlJobRegistry inFlightCrawlJobRegistry;

    @Test
    @DisplayName("should record retry metric when worker schedules a retry")
    void shouldRecordRetryMetricWhenWorkerSchedulesARetry() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        CrawlJobEntity crawlJob = buildPersistedJob();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(crawlJob));
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.FAILED);

        CrawlJobWorker worker = newWorker(meterRegistry);

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        assertThat(meterRegistry.get("webscraper.crawl.worker.total")
                .tag("queue", "API_JOBS")
                .tag("outcome", "retry_scheduled")
                .tag("site_code", "indeed-br")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("should consume persisted queued job and dispatch it")
    void shouldConsumePersistedQueuedJobAndDispatchIt() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        CrawlJobEntity crawlJob = buildPersistedJob();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(crawlJob));
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.SUCCEEDED);

        CrawlJobWorker worker = newWorker();

        boolean consumed = worker.consumeNext(CrawlJobQueueName.API_JOBS);

        assertThat(consumed).isTrue();
        ArgumentCaptor<CrawlJobEntity> jobCaptor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(crawlJobDispatcher).dispatch(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(jobCaptor.getValue().getTargetSite().getSiteCode()).isEqualTo("indeed-br");
        assertThat(jobCaptor.getValue().getTargetSite().getBaseUrl()).isEqualTo("https://br.indeed.com/jobs?q=java");
        verify(crawlJobQueue).markDone(message);
        verify(inFlightCrawlJobRegistry).release(10L);
    }

    @Test
    @DisplayName("should persist transient queued job before dispatching it")
    void shouldPersistTransientQueuedJobBeforeDispatchingIt() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                null,
                200L,
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        TargetSiteEntity targetSite = buildPersistedTargetSite(200L, "greenhouse_bitso");
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(targetSiteRepository.findById(200L)).thenReturn(Optional.of(targetSite));
        when(crawlJobRepository.save(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlJobEntity transientJob = invocation.getArgument(0);
                    return CrawlJobEntity.builder()
                            .id(20L)
                            .targetSite(transientJob.getTargetSite())
                            .scheduledAt(transientJob.getScheduledAt())
                            .jobCategory(transientJob.getJobCategory())
                            .createdAt(transientJob.getCreatedAt())
                            .build();
                });
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.SUCCEEDED);

        CrawlJobWorker worker = newWorker();

        boolean consumed = worker.consumeNext(CrawlJobQueueName.API_JOBS);

        assertThat(consumed).isTrue();
        verify(crawlJobRepository, never()).findById(anyLong());
        verify(targetSiteRepository).findById(200L);
        verify(crawlJobRepository).save(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class));

        ArgumentCaptor<CrawlJobEntity> jobCaptor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(crawlJobDispatcher).dispatch(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getId()).isEqualTo(20L);
        assertThat(jobCaptor.getValue().getTargetSite().getId()).isEqualTo(200L);
        assertThat(jobCaptor.getValue().getTargetSite().getSiteCode()).isEqualTo("greenhouse_bitso");
        assertThat(jobCaptor.getValue().getTargetSite().getBaseUrl())
                .isEqualTo("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true");
    }

    @Test
    @DisplayName("should move transient queued job without target site id to dead letter")
    void shouldMoveTransientQueuedJobWithoutTargetSiteIdToDeadLetter() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                null,
                null,
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(crawlJobQueue).moveToDeadLetter(message, "targetSiteId must not be null for transient queued jobs");
        verify(crawlJobDispatcher, never()).dispatch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("should move message to dead letter when persisted crawl job no longer exists")
    void shouldMoveMessageToDeadLetterWhenPersistedCrawlJobNoLongerExists() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.empty());

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(crawlJobQueue).moveToDeadLetter(message, "Crawl job not found: 10");
        verify(crawlJobDispatcher, never()).dispatch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("should skip queued job when target site is currently disabled")
    void shouldSkipQueuedJobWhenTargetSiteIsCurrentlyDisabled() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        CrawlJobEntity disabledJob = CrawlJobEntity.builder()
                .id(10L)
                .targetSite(TargetSiteEntity.builder()
                        .id(100L)
                        .siteCode("indeed-br")
                        .displayName("indeed-br")
                        .baseUrl("https://indeed-br.example.com")
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(false)
                        .createdAt(Instant.parse("2026-03-13T17:59:00Z"))
                        .build())
                .scheduledAt(Instant.parse("2026-03-13T18:00:00Z"))
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(Instant.parse("2026-03-13T17:59:30Z"))
                .build();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(disabledJob));

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(crawlJobQueue).moveToDeadLetter(message, "Target site disabled after enqueue");
        verify(crawlJobDispatcher, never()).dispatch(org.mockito.ArgumentMatchers.any());
        verify(inFlightCrawlJobRegistry).release(10L);
    }

    @Test
    @DisplayName("should requeue message in original queue when dispatcher fails transiently")
    void shouldRequeueMessageInOriginalQueueWhenDispatcherFailsTransiently() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        CrawlJobEntity crawlJob = buildPersistedJob();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(crawlJob));
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenThrow(new IllegalStateException("temporary dispatcher outage"));

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        ArgumentCaptor<EnqueuedCrawlJob> envelopeCaptor = ArgumentCaptor.forClass(EnqueuedCrawlJob.class);
        ArgumentCaptor<Instant> retryAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(crawlJobQueue).scheduleRetry(envelopeCaptor.capture(), retryAtCaptor.capture(), org.mockito.ArgumentMatchers.eq("Transient execution failure"));
        assertThat(envelopeCaptor.getValue().retryCount()).isEqualTo(0);
        assertThat(envelopeCaptor.getValue().targetUrl()).isEqualTo(message.targetUrl());
        assertThat(retryAtCaptor.getValue()).isAfter(message.availableAt());
    }

    @Test
    @DisplayName("should move transient message to dead letter when target site no longer exists")
    void shouldMoveTransientMessageToDeadLetterWhenTargetSiteNoLongerExists() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                null,
                200L,
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(targetSiteRepository.findById(200L)).thenReturn(Optional.empty());

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(crawlJobQueue).moveToDeadLetter(message, "Target site not found for queued job: 200");
        verify(crawlJobDispatcher, never()).dispatch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("should drain all production queues")
    void shouldDrainAllProductionQueues() {
        CrawlJobWorker worker = newWorker();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.empty());
        when(crawlJobQueue.consume(CrawlJobQueueName.STATIC_SCRAPE_JOBS)).thenReturn(Optional.empty());
        when(crawlJobQueue.consume(CrawlJobQueueName.DYNAMIC_BROWSER_JOBS)).thenReturn(Optional.empty());

        worker.drainQueues();

        verify(crawlJobQueue).consume(CrawlJobQueueName.API_JOBS);
        verify(crawlJobQueue).consume(CrawlJobQueueName.STATIC_SCRAPE_JOBS);
        verify(crawlJobQueue).consume(CrawlJobQueueName.DYNAMIC_BROWSER_JOBS);
    }

    @Test
    @DisplayName("should requeue message in original queue when execution finishes as failed")
    void shouldRequeueMessageInOriginalQueueWhenExecutionFinishesAsFailed() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        CrawlJobEntity crawlJob = buildPersistedJob();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(crawlJob));
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.FAILED);

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        ArgumentCaptor<EnqueuedCrawlJob> envelopeCaptor = ArgumentCaptor.forClass(EnqueuedCrawlJob.class);
        ArgumentCaptor<Instant> retryAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(crawlJobQueue).scheduleRetry(envelopeCaptor.capture(), retryAtCaptor.capture(), org.mockito.ArgumentMatchers.eq("Transient execution failure"));
        assertThat(envelopeCaptor.getValue().retryCount()).isEqualTo(0);
        assertThat(retryAtCaptor.getValue()).isAfter(message.availableAt());
    }

    @Test
    @DisplayName("should requeue transient message with persisted crawl job id after failed execution")
    void shouldRequeueTransientMessageWithPersistedCrawlJobIdAfterFailedExecution() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                null,
                200L,
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        TargetSiteEntity targetSite = buildPersistedTargetSite(200L, "greenhouse_bitso");
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(targetSiteRepository.findById(200L)).thenReturn(Optional.of(targetSite));
        when(crawlJobRepository.save(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlJobEntity transientJob = invocation.getArgument(0);
                    return CrawlJobEntity.builder()
                            .id(20L)
                            .targetSite(transientJob.getTargetSite())
                            .scheduledAt(transientJob.getScheduledAt())
                            .jobCategory(transientJob.getJobCategory())
                            .createdAt(transientJob.getCreatedAt())
                            .build();
                });
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.FAILED);

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();

        ArgumentCaptor<EnqueuedCrawlJob> envelopeCaptor = ArgumentCaptor.forClass(EnqueuedCrawlJob.class);
        ArgumentCaptor<Instant> retryAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(crawlJobQueue).scheduleRetry(envelopeCaptor.capture(), retryAtCaptor.capture(), org.mockito.ArgumentMatchers.eq("Transient execution failure"));
        assertThat(envelopeCaptor.getValue().crawlJobId()).isEqualTo(20L);
        assertThat(envelopeCaptor.getValue().targetSiteId()).isEqualTo(200L);
        assertThat(retryAtCaptor.getValue()).isAfter(message.availableAt());
    }

    @Test
    @DisplayName("should preserve queued target url override when retrying persisted transient job")
    void shouldPreserveQueuedTargetUrlOverrideWhenRetryingPersistedTransientJob() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                20L,
                200L,
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true&query=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        TargetSiteEntity persistedTargetSite = buildPersistedTargetSite(200L, "greenhouse_bitso");
        CrawlJobEntity persistedJob = CrawlJobEntity.builder()
                .id(20L)
                .targetSite(persistedTargetSite)
                .scheduledAt(Instant.parse("2026-03-13T18:00:00Z"))
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(Instant.parse("2026-03-13T18:01:00Z"))
                .build();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(20L)).thenReturn(Optional.of(persistedJob));
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.FAILED);

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();

        ArgumentCaptor<CrawlJobEntity> jobCaptor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(crawlJobDispatcher).dispatch(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getTargetSite().getBaseUrl())
                .isEqualTo("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true&query=java");
        ArgumentCaptor<EnqueuedCrawlJob> envelopeCaptor = ArgumentCaptor.forClass(EnqueuedCrawlJob.class);
        ArgumentCaptor<Instant> retryAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(crawlJobQueue).scheduleRetry(envelopeCaptor.capture(), retryAtCaptor.capture(), org.mockito.ArgumentMatchers.eq("Transient execution failure"));
        assertThat(envelopeCaptor.getValue().crawlJobId()).isEqualTo(20L);
        assertThat(envelopeCaptor.getValue().retryCount()).isEqualTo(0);
        assertThat(envelopeCaptor.getValue().targetUrl()).isEqualTo(message.targetUrl());
        assertThat(retryAtCaptor.getValue()).isAfter(message.availableAt());
    }

    @Test
    @DisplayName("should move dispatcher dead-letter executions to dead-letter queue")
    void shouldMoveDispatcherDeadLetterExecutionsToDeadLetterQueue() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        CrawlJobEntity crawlJob = buildPersistedJob();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(crawlJob));
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.DEAD_LETTER);

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(crawlJobQueue).moveToDeadLetter(message, "Dispatcher moved execution to dead letter");
        verify(inFlightCrawlJobRegistry).release(10L);
    }

    @Test
    @DisplayName("should move message to dead letter when retry limit is exhausted")
    void shouldMoveMessageToDeadLetterWhenRetryLimitIsExhausted() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                10L,
                100L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                CrawlJobWorker.MAX_RETRIES,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        CrawlJobEntity crawlJob = buildPersistedJob();
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.of(message));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(crawlJob));
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.FAILED);

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(crawlJobQueue).moveToDeadLetter(message, "Retry limit exhausted");
        verify(inFlightCrawlJobRegistry).release(10L);
    }

    @Test
    @DisplayName("should inherit target site job category for transient public contest jobs")
    void shouldInheritTargetSiteJobCategoryForTransientPublicContestJobs() {
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                null,
                300L,
                "pci_concursos",
                "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao",
                ExtractionMode.STATIC_HTML,
                null,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.STATIC_SCRAPE_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );
        TargetSiteEntity targetSite = buildPersistedPublicContestTargetSite(300L, "pci_concursos");
        when(crawlJobQueue.consume(CrawlJobQueueName.STATIC_SCRAPE_JOBS)).thenReturn(Optional.of(message));
        when(targetSiteRepository.findById(300L)).thenReturn(Optional.of(targetSite));
        when(crawlJobRepository.save(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenAnswer(invocation -> {
                    CrawlJobEntity transientJob = invocation.getArgument(0);
                    return CrawlJobEntity.builder()
                            .id(30L)
                            .targetSite(transientJob.getTargetSite())
                            .scheduledAt(transientJob.getScheduledAt())
                            .jobCategory(transientJob.getJobCategory())
                            .createdAt(transientJob.getCreatedAt())
                            .build();
                });
        when(crawlJobDispatcher.dispatch(org.mockito.ArgumentMatchers.any(CrawlJobEntity.class)))
                .thenReturn(CrawlExecutionStatus.SUCCEEDED);

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.STATIC_SCRAPE_JOBS)).isTrue();

        ArgumentCaptor<CrawlJobEntity> savedJobCaptor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(crawlJobRepository).save(savedJobCaptor.capture());
        assertThat(savedJobCaptor.getValue().getJobCategory()).isEqualTo(JobCategory.PUBLIC_CONTEST);
        assertThat(savedJobCaptor.getValue().isSchedulerManaged()).isFalse();

        ArgumentCaptor<CrawlJobEntity> dispatchedJobCaptor = ArgumentCaptor.forClass(CrawlJobEntity.class);
        verify(crawlJobDispatcher).dispatch(dispatchedJobCaptor.capture());
        assertThat(dispatchedJobCaptor.getValue().getJobCategory()).isEqualTo(JobCategory.PUBLIC_CONTEST);
        assertThat(dispatchedJobCaptor.getValue().getTargetSite().getJobCategory()).isEqualTo(JobCategory.PUBLIC_CONTEST);
    }

    @Test
    @DisplayName("should return false when queue is empty")
    void shouldReturnFalseWhenQueueIsEmpty() {
        when(crawlJobQueue.consume(CrawlJobQueueName.API_JOBS)).thenReturn(Optional.empty());

        CrawlJobWorker worker = newWorker();

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(crawlJobDispatcher, never()).dispatch(org.mockito.ArgumentMatchers.any());
    }

    private CrawlJobWorker newWorker() {
        return newWorker(new SimpleMeterRegistry());
    }

    private CrawlJobWorker newWorker(SimpleMeterRegistry meterRegistry) {
        return new CrawlJobWorker(
                crawlJobQueue,
                crawlJobRepository,
                targetSiteRepository,
                crawlJobDispatcher,
                inFlightCrawlJobRegistry,
                new CrawlObservabilityService(meterRegistry)
        );
    }

    private static CrawlJobEntity buildPersistedJob() {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(10L)
                .targetSite(buildPersistedTargetSite(100L, "indeed-br"))
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now.minusSeconds(30))
                .build();
    }

    private static TargetSiteEntity buildPersistedTargetSite(Long id, String siteCode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return TargetSiteEntity.builder()
                .id(id)
                .siteCode(siteCode)
                .displayName(siteCode)
                .baseUrl("https://" + siteCode + ".example.com")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(now.minusSeconds(60))
                .build();
    }

    private static TargetSiteEntity buildPersistedPublicContestTargetSite(Long id, String siteCode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return TargetSiteEntity.builder()
                .id(id)
                .siteCode(siteCode)
                .displayName(siteCode)
                .baseUrl("https://" + siteCode + ".example.com")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("pci_concursos_v1")
                .enabled(true)
                .createdAt(now.minusSeconds(60))
                .build();
    }
}
