package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.application.queue.CrawlJobQueueRouter;
import com.campos.webscraper.application.queue.InFlightCrawlJobRegistry;
import com.campos.webscraper.application.queue.PersistentCrawlJobQueue;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.PersistentQueueMessageRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.interfaces.scheduler.CrawlJobScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Persistent queue workflow")
class PersistentQueueWorkflowTest {

    @Mock
    private PersistentQueueMessageRepository persistentQueueMessageRepository;

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Mock
    private TargetSiteRepository targetSiteRepository;

    @Mock
    private CrawlJobDispatcher crawlJobDispatcher;

    @Mock
    private InFlightCrawlJobRegistry inFlightCrawlJobRegistry;

    @Test
    @DisplayName("should execute due job through scheduler queue and worker using persistent lifecycle")
    void shouldExecuteDueJobThroughSchedulerQueueAndWorkerUsingPersistentLifecycle() {
        Instant now = Instant.parse("2026-03-17T15:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PersistentCrawlJobQueue queue = new PersistentCrawlJobQueue(
                persistentQueueMessageRepository,
                objectMapper,
                fixedClock
        );
        CrawlJobEntity dueJob = buildJob(10L, "indeed-br", ExtractionMode.API, now.minusSeconds(60));

        when(crawlJobRepository.findByTargetSiteEnabledTrueAndSchedulerManagedTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now))
                .thenReturn(List.of(dueJob));
        when(inFlightCrawlJobRegistry.tryClaim(10L)).thenReturn(true);
        when(persistentQueueMessageRepository.save(any(PersistentQueueMessageEntity.class)))
                .thenAnswer(invocation -> {
                    PersistentQueueMessageEntity message = invocation.getArgument(0);
                    return message.toBuilder().id(700L).build();
                });

        CrawlJobScheduler scheduler = new CrawlJobScheduler(
                crawlJobRepository,
                queue,
                new CrawlJobQueueRouter(),
                inFlightCrawlJobRegistry,
                fixedClock
        );

        scheduler.triggerDueJobs();

        ArgumentCaptor<PersistentQueueMessageEntity> persistedCaptor =
                ArgumentCaptor.forClass(PersistentQueueMessageEntity.class);
        verify(persistentQueueMessageRepository).save(persistedCaptor.capture());

        when(persistentQueueMessageRepository.claimNextReadyMessage(CrawlJobQueueName.API_JOBS, now))
                .thenReturn(Optional.of(persistedCaptor.getValue().toBuilder()
                        .id(700L)
                        .status(QueueMessageStatus.CLAIMED)
                        .claimedAt(now)
                        .updatedAt(now)
                        .build()));
        when(crawlJobRepository.findById(10L)).thenReturn(Optional.of(dueJob));
        when(crawlJobDispatcher.dispatch(any(CrawlJobEntity.class))).thenReturn(CrawlExecutionStatus.SUCCEEDED);

        CrawlJobWorker worker = new CrawlJobWorker(
                queue,
                crawlJobRepository,
                targetSiteRepository,
                crawlJobDispatcher,
                inFlightCrawlJobRegistry,
                new CrawlObservabilityService(new SimpleMeterRegistry())
        );

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isTrue();
        verify(persistentQueueMessageRepository).markDone(700L, now);
        verify(inFlightCrawlJobRegistry).release(10L);
    }

    @Test
    @DisplayName("should keep retry state inside persistent queue when execution fails")
    void shouldKeepRetryStateInsidePersistentQueueWhenExecutionFails() {
        Instant now = Instant.parse("2026-03-17T15:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PersistentCrawlJobQueue queue = new PersistentCrawlJobQueue(
                persistentQueueMessageRepository,
                objectMapper,
                fixedClock
        );
        CrawlJobEntity dueJob = buildJob(20L, "greenhouse_bitso", ExtractionMode.API, now.minusSeconds(60));

        when(crawlJobRepository.findByTargetSiteEnabledTrueAndSchedulerManagedTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now))
                .thenReturn(List.of(dueJob));
        when(inFlightCrawlJobRegistry.tryClaim(20L)).thenReturn(true);
        when(persistentQueueMessageRepository.save(any(PersistentQueueMessageEntity.class)))
                .thenAnswer(invocation -> {
                    PersistentQueueMessageEntity message = invocation.getArgument(0);
                    return message.toBuilder().id(701L).build();
                });

        CrawlJobScheduler scheduler = new CrawlJobScheduler(
                crawlJobRepository,
                queue,
                new CrawlJobQueueRouter(),
                inFlightCrawlJobRegistry,
                fixedClock
        );

        scheduler.triggerDueJobs();

        ArgumentCaptor<PersistentQueueMessageEntity> persistedCaptor =
                ArgumentCaptor.forClass(PersistentQueueMessageEntity.class);
        verify(persistentQueueMessageRepository).save(persistedCaptor.capture());

        when(persistentQueueMessageRepository.claimNextReadyMessage(CrawlJobQueueName.API_JOBS, now))
                .thenReturn(Optional.of(persistedCaptor.getValue().toBuilder()
                        .id(701L)
                        .status(QueueMessageStatus.CLAIMED)
                        .claimedAt(now)
                        .updatedAt(now)
                        .build()));
        when(crawlJobRepository.findById(20L)).thenReturn(Optional.of(dueJob));
        when(crawlJobDispatcher.dispatch(any(CrawlJobEntity.class))).thenReturn(CrawlExecutionStatus.FAILED);

        CrawlJobWorker worker = new CrawlJobWorker(
                queue,
                crawlJobRepository,
                targetSiteRepository,
                crawlJobDispatcher,
                inFlightCrawlJobRegistry,
                new CrawlObservabilityService(new SimpleMeterRegistry())
        );

        assertThat(worker.consumeNext(CrawlJobQueueName.API_JOBS)).isFalse();
        verify(persistentQueueMessageRepository).scheduleRetry(
                eq(701L),
                org.mockito.ArgumentMatchers.contains("\"retryCount\":1"),
                any(Instant.class),
                eq(now),
                eq("Transient execution failure")
        );
    }

    private static CrawlJobEntity buildJob(Long id, String siteCode, ExtractionMode extractionMode, Instant scheduledAt) {
        Instant createdAt = scheduledAt.minusSeconds(60);
        return CrawlJobEntity.builder()
                .id(id)
                .targetSite(TargetSiteEntity.builder()
                        .id(id + 1_000)
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://" + siteCode + ".example.com")
                        .siteType(extractionMode == ExtractionMode.API ? SiteType.TYPE_E : SiteType.TYPE_A)
                        .extractionMode(extractionMode)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(createdAt.minusSeconds(60))
                        .build())
                .scheduledAt(scheduledAt)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(createdAt)
                .build();
    }
}
