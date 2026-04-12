package com.campos.webscraper.domain.repository;

import com.campos.webscraper.TestcontainersConfiguration;
import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryPersistenceTest
@DisplayName("PersistentQueueMessageRepository integration")
class PersistentQueueMessageRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = TestcontainersConfiguration.newPostgresContainer();

    @Autowired
    private PersistentQueueMessageRepository persistentQueueMessageRepository;

    @BeforeEach
    void setUp() {
        resetPersistentQueueMessages();
    }

    @Test
    @DisplayName("should claim next ready message atomically by queue name and availableAt")
    void shouldClaimNextReadyMessageAtomicallyByQueueNameAndAvailableAt() {
        Instant now = Instant.parse("2026-03-16T15:00:00Z");
        PersistentQueueMessageEntity firstReady = persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.API_JOBS,
                QueueMessageStatus.READY,
                now.minusSeconds(60),
                "{\"crawlJobId\":1}"
        ));
        persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.API_JOBS,
                QueueMessageStatus.RETRY_WAIT,
                now.minusSeconds(30),
                "{\"crawlJobId\":2}"
        ));
        persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.API_JOBS,
                QueueMessageStatus.READY,
                now.plusSeconds(60),
                "{\"crawlJobId\":3}"
        ));
        persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.API_JOBS,
                QueueMessageStatus.READY,
                now.minusSeconds(10),
                "{\"crawlJobId\":4}"
        ));
        persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.STATIC_SCRAPE_JOBS,
                QueueMessageStatus.READY,
                now.minusSeconds(20),
                "{\"crawlJobId\":5}"
        ));

        PersistentQueueMessageEntity claimedMessage = persistentQueueMessageRepository
                .claimNextReadyMessage(CrawlJobQueueName.API_JOBS, now)
                .orElseThrow();

        assertThat(claimedMessage.getId()).isEqualTo(firstReady.getId());
        assertThat(claimedMessage.getStatus()).isEqualTo(QueueMessageStatus.CLAIMED);
        assertThat(claimedMessage.getClaimedAt()).isEqualTo(now);

        PersistentQueueMessageEntity reloadedFirstMessage = persistentQueueMessageRepository
                .findById(firstReady.getId())
                .orElseThrow();

        assertThat(reloadedFirstMessage.getStatus()).isEqualTo(QueueMessageStatus.CLAIMED);
        assertThat(reloadedFirstMessage.getClaimedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should return empty when no ready message is due for the queue")
    void shouldReturnEmptyWhenNoReadyMessageIsDueForTheQueue() {
        Instant now = Instant.parse("2026-03-16T15:00:00Z");
        persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.API_JOBS,
                QueueMessageStatus.RETRY_WAIT,
                now.plusSeconds(30),
                "{\"crawlJobId\":2}"
        ));
        persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.API_JOBS,
                QueueMessageStatus.READY,
                now.plusSeconds(60),
                "{\"crawlJobId\":3}"
        ));

        assertThat(persistentQueueMessageRepository.claimNextReadyMessage(CrawlJobQueueName.API_JOBS, now)).isEmpty();
    }

    @Test
    @DisplayName("should re-claim a due retry wait message once its backoff expires")
    void shouldReClaimADueRetryWaitMessageOnceItsBackoffExpires() {
        Instant now = Instant.parse("2026-03-16T15:00:00Z");
        PersistentQueueMessageEntity retriedMessage = persistentQueueMessageRepository.save(
                buildMessage(
                        CrawlJobQueueName.API_JOBS,
                        QueueMessageStatus.RETRY_WAIT,
                        now.minusSeconds(30),
                        "{\"crawlJobId\":6}"
                ).toBuilder()
                        .retryCount(2)
                        .lastError("temporary upstream failure")
                        .build()
        );

        PersistentQueueMessageEntity claimedMessage = persistentQueueMessageRepository
                .claimNextReadyMessage(CrawlJobQueueName.API_JOBS, now)
                .orElseThrow();

        assertThat(claimedMessage.getId()).isEqualTo(retriedMessage.getId());
        assertThat(claimedMessage.getStatus()).isEqualTo(QueueMessageStatus.CLAIMED);
        assertThat(claimedMessage.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should mark a claimed message as done")
    void shouldMarkAClaimedMessageAsDone() {
        Instant now = Instant.parse("2026-03-16T15:00:00Z");
        PersistentQueueMessageEntity claimedMessage = persistentQueueMessageRepository.save(buildMessage(
                CrawlJobQueueName.API_JOBS,
                QueueMessageStatus.CLAIMED,
                now.minusSeconds(60),
                "{\"crawlJobId\":10}"
        ));

        PersistentQueueMessageEntity updated = persistentQueueMessageRepository
                .markDone(claimedMessage.getId(), now)
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(QueueMessageStatus.DONE);
        assertThat(updated.getUpdatedAt()).isEqualTo(now);
        assertThat(updated.getClaimedAt()).isNull();
    }

    @Test
    @DisplayName("should move a claimed message to retry wait and increment retry count")
    void shouldMoveAClaimedMessageToRetryWaitAndIncrementRetryCount() {
        Instant now = Instant.parse("2026-03-16T15:00:00Z");
        Instant nextAttempt = now.plusSeconds(300);
        String retriedPayload = """
                {"crawlJobId":11,"retryCount":2,"availableAt":"2026-03-16T15:05:00Z"}
                """;
        PersistentQueueMessageEntity claimedMessage = persistentQueueMessageRepository.save(
                buildMessage(
                        CrawlJobQueueName.API_JOBS,
                        QueueMessageStatus.CLAIMED,
                        now.minusSeconds(60),
                        "{\"crawlJobId\":11}"
                ).toBuilder()
                        .retryCount(1)
                        .claimedAt(now.minusSeconds(30))
                        .build()
        );

        PersistentQueueMessageEntity updated = persistentQueueMessageRepository
                .scheduleRetry(
                        claimedMessage.getId(),
                        retriedPayload,
                        nextAttempt,
                        now,
                        "upstream timeout"
                )
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(QueueMessageStatus.RETRY_WAIT);
        assertThat(updated.getRetryCount()).isEqualTo(2);
        assertThat(updated.getAvailableAt()).isEqualTo(nextAttempt);
        assertThat(updated.getLastError()).isEqualTo("upstream timeout");
        assertThat(updated.getClaimedAt()).isNull();
        assertThat(updated.getUpdatedAt()).isEqualTo(now);
        assertThat(updated.getPayloadJson()).isEqualTo(retriedPayload);
    }

    @Test
    @DisplayName("should move a claimed message to dead letter")
    void shouldMoveAClaimedMessageToDeadLetter() {
        Instant now = Instant.parse("2026-03-16T15:00:00Z");
        PersistentQueueMessageEntity claimedMessage = persistentQueueMessageRepository.save(
                buildMessage(
                        CrawlJobQueueName.STATIC_SCRAPE_JOBS,
                        QueueMessageStatus.CLAIMED,
                        now.minusSeconds(60),
                        "{\"crawlJobId\":12}"
                ).toBuilder()
                        .claimedAt(now.minusSeconds(15))
                        .build()
        );

        PersistentQueueMessageEntity updated = persistentQueueMessageRepository
                .moveToDeadLetter(
                        claimedMessage.getId(),
                        "{\"crawlJobId\":12,\"queueName\":\"DEAD_LETTER_JOBS\"}",
                        now,
                        "unsupported payload"
                )
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(QueueMessageStatus.DEAD_LETTER);
        assertThat(updated.getLastError()).isEqualTo("unsupported payload");
        assertThat(updated.getUpdatedAt()).isEqualTo(now);
        assertThat(updated.getClaimedAt()).isNull();
    }

    private static PersistentQueueMessageEntity buildMessage(
            CrawlJobQueueName queueName,
            QueueMessageStatus status,
            Instant availableAt,
            String payloadJson
    ) {
        return PersistentQueueMessageEntity.builder()
                .queueName(queueName)
                .payloadJson(payloadJson)
                .status(status)
                .availableAt(availableAt)
                .createdAt(availableAt.minusSeconds(30))
                .updatedAt(availableAt.minusSeconds(10))
                .build();
    }
}
