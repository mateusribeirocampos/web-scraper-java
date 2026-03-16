package com.campos.webscraper.domain.repository;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=none"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("PersistentQueueMessageRepository integration")
class PersistentQueueMessageRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private PersistentQueueMessageRepository persistentQueueMessageRepository;

    @BeforeEach
    void setUp() {
        persistentQueueMessageRepository.deleteAll();
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
                now.minusSeconds(30),
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
