package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.PersistentQueueMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("PersistentCrawlJobQueue")
class PersistentCrawlJobQueueTest {

    private final PersistentQueueMessageRepository persistentQueueMessageRepository =
            mock(PersistentQueueMessageRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-16T20:00:00Z"), ZoneOffset.UTC);
    private final PersistentCrawlJobQueue queue = new PersistentCrawlJobQueue(
            persistentQueueMessageRepository,
            objectMapper,
            clock
    );

    @Test
    @DisplayName("should persist a ready queue message when enqueueing a crawl job entity")
    void shouldPersistAReadyQueueMessageWhenEnqueueingACrawlJobEntity() {
        CrawlJobEntity crawlJob = buildJob(1L, "indeed-br", ExtractionMode.API);
        when(persistentQueueMessageRepository.save(any(PersistentQueueMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnqueuedCrawlJob message = queue.enqueue(crawlJob, CrawlJobQueueName.API_JOBS);

        ArgumentCaptor<PersistentQueueMessageEntity> captor =
                ArgumentCaptor.forClass(PersistentQueueMessageEntity.class);
        verify(persistentQueueMessageRepository).save(captor.capture());

        PersistentQueueMessageEntity persistedMessage = captor.getValue();
        assertThat(message.crawlJobId()).isEqualTo(1L);
        assertThat(persistedMessage.getQueueName()).isEqualTo(CrawlJobQueueName.API_JOBS);
        assertThat(persistedMessage.getStatus()).isEqualTo(QueueMessageStatus.READY);
        assertThat(persistedMessage.getRetryCount()).isEqualTo(0);
        assertThat(persistedMessage.getAvailableAt()).isEqualTo(Instant.parse("2026-03-16T20:00:00Z"));
        assertThat(persistedMessage.getPayloadJson()).contains("\"crawlJobId\":1");
        assertThat(persistedMessage.getPayloadJson()).contains("\"targetSiteCode\":\"indeed-br\"");
    }

    @Test
    @DisplayName("should persist a re-enqueued envelope with the overridden queue name")
    void shouldPersistAReEnqueuedEnvelopeWithTheOverriddenQueueName() {
        EnqueuedCrawlJob original = new EnqueuedCrawlJob(
                null,
                2L,
                1002L,
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-16T19:55:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-16T19:55:10Z"),
                1,
                Instant.parse("2026-03-16T20:05:00Z")
        );
        when(persistentQueueMessageRepository.save(any(PersistentQueueMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnqueuedCrawlJob requeued = queue.enqueue(original, CrawlJobQueueName.DEAD_LETTER_JOBS);

        ArgumentCaptor<PersistentQueueMessageEntity> captor =
                ArgumentCaptor.forClass(PersistentQueueMessageEntity.class);
        verify(persistentQueueMessageRepository).save(captor.capture());

        assertThat(requeued.queueName()).isEqualTo(CrawlJobQueueName.DEAD_LETTER_JOBS);
        assertThat(captor.getValue().getQueueName()).isEqualTo(CrawlJobQueueName.DEAD_LETTER_JOBS);
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        assertThat(captor.getValue().getPayloadJson()).contains("\"queueName\":\"DEAD_LETTER_JOBS\"");
    }

    @Test
    @DisplayName("should consume the next claimed message from persistent storage")
    void shouldConsumeTheNextClaimedMessageFromPersistentStorage() {
        EnqueuedCrawlJob expected = new EnqueuedCrawlJob(
                30L,
                3L,
                1003L,
                "pci_concursos",
                "https://www.pciconcursos.com.br/professores/",
                ExtractionMode.STATIC_HTML,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-16T19:55:00Z"),
                CrawlJobQueueName.STATIC_SCRAPE_JOBS,
                Instant.parse("2026-03-16T19:55:10Z"),
                0,
                Instant.parse("2026-03-16T19:55:10Z")
        );
        when(persistentQueueMessageRepository.claimNextReadyMessage(
                eq(CrawlJobQueueName.STATIC_SCRAPE_JOBS),
                eq(Instant.parse("2026-03-16T20:00:00Z"))
        )).thenReturn(Optional.of(PersistentQueueMessageEntity.builder()
                .id(30L)
                .queueName(CrawlJobQueueName.STATIC_SCRAPE_JOBS)
                .payloadJson(writeJson(expected))
                .status(QueueMessageStatus.CLAIMED)
                .availableAt(expected.availableAt())
                .claimedAt(Instant.parse("2026-03-16T20:00:00Z"))
                .retryCount(0)
                .createdAt(Instant.parse("2026-03-16T19:55:10Z"))
                .updatedAt(Instant.parse("2026-03-16T20:00:00Z"))
                .build()));

        Optional<EnqueuedCrawlJob> consumed = queue.consume(CrawlJobQueueName.STATIC_SCRAPE_JOBS);

        assertThat(consumed).contains(expected);
    }

    @Test
    @DisplayName("should return empty when no persistent message is claimable")
    void shouldReturnEmptyWhenNoPersistentMessageIsClaimable() {
        when(persistentQueueMessageRepository.claimNextReadyMessage(
                eq(CrawlJobQueueName.API_JOBS),
                eq(Instant.parse("2026-03-16T20:00:00Z"))
        )).thenReturn(Optional.empty());

        assertThat(queue.consume(CrawlJobQueueName.API_JOBS)).isEmpty();
    }

    @Test
    @DisplayName("should mark a claimed persistent message as done")
    void shouldMarkAClaimedPersistentMessageAsDone() {
        EnqueuedCrawlJob claimed = new EnqueuedCrawlJob(
                40L,
                3L,
                1003L,
                "pci_concursos",
                "https://www.pciconcursos.com.br/professores/",
                ExtractionMode.STATIC_HTML,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-16T19:55:00Z"),
                CrawlJobQueueName.STATIC_SCRAPE_JOBS,
                Instant.parse("2026-03-16T19:55:10Z"),
                0,
                Instant.parse("2026-03-16T19:55:10Z")
        );

        queue.markDone(claimed);

        verify(persistentQueueMessageRepository).markDone(40L, Instant.parse("2026-03-16T20:00:00Z"));
    }

    @Test
    @DisplayName("should update persistent retry metadata instead of re-enqueueing a new row")
    void shouldUpdatePersistentRetryMetadataInsteadOfReEnqueueingANewRow() {
        EnqueuedCrawlJob claimed = new EnqueuedCrawlJob(
                50L,
                4L,
                1004L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-16T19:55:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-16T19:55:10Z"),
                0,
                Instant.parse("2026-03-16T19:55:10Z")
        );
        Instant nextAttempt = Instant.parse("2026-03-16T20:05:00Z");

        EnqueuedCrawlJob retried = queue.scheduleRetry(claimed, nextAttempt, "temporary outage");

        assertThat(retried.retryCount()).isEqualTo(1);
        assertThat(retried.availableAt()).isEqualTo(nextAttempt);
        verify(persistentQueueMessageRepository).scheduleRetry(
                eq(50L),
                org.mockito.ArgumentMatchers.contains("\"retryCount\":1"),
                eq(nextAttempt),
                eq(Instant.parse("2026-03-16T20:00:00Z")),
                eq("temporary outage")
        );
        verify(persistentQueueMessageRepository, never()).save(any(PersistentQueueMessageEntity.class));
    }

    @Test
    @DisplayName("should move a claimed persistent message to dead letter using repository transition")
    void shouldMoveAClaimedPersistentMessageToDeadLetterUsingRepositoryTransition() {
        EnqueuedCrawlJob claimed = new EnqueuedCrawlJob(
                60L,
                5L,
                1005L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-16T19:55:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-16T19:55:10Z"),
                0,
                Instant.parse("2026-03-16T19:55:10Z")
        );

        EnqueuedCrawlJob deadLetter = queue.moveToDeadLetter(claimed, "fatal upstream error");

        assertThat(deadLetter.queueName()).isEqualTo(CrawlJobQueueName.DEAD_LETTER_JOBS);
        verify(persistentQueueMessageRepository).moveToDeadLetter(
                eq(60L),
                org.mockito.ArgumentMatchers.contains("\"queueName\":\"DEAD_LETTER_JOBS\""),
                eq(Instant.parse("2026-03-16T20:00:00Z")),
                eq("fatal upstream error")
        );
    }

    private String writeJson(EnqueuedCrawlJob message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static CrawlJobEntity buildJob(Long id, String siteCode, ExtractionMode extractionMode) {
        Instant now = Instant.parse("2026-03-16T20:00:00Z");
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
                        .createdAt(now.minusSeconds(600))
                        .build())
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now.minusSeconds(60))
                .build();
    }
}
