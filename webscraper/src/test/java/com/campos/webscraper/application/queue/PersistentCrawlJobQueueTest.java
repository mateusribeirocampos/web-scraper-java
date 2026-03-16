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
