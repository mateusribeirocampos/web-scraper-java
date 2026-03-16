package com.campos.webscraper.domain.model;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PersistentQueueMessageEntity")
class PersistentQueueMessageEntityTest {

    @Test
    @DisplayName("builder should preserve durable queue fields")
    void builderShouldPreserveDurableQueueFields() {
        Instant now = Instant.parse("2026-03-16T14:00:00Z");

        PersistentQueueMessageEntity message = PersistentQueueMessageEntity.builder()
                .id(1L)
                .queueName(CrawlJobQueueName.API_JOBS)
                .payloadJson("{\"crawlJobId\":10}")
                .status(QueueMessageStatus.READY)
                .availableAt(now)
                .retryCount(2)
                .lastError("timeout")
                .createdAt(now.minusSeconds(30))
                .updatedAt(now)
                .build();

        assertThat(message.getId()).isEqualTo(1L);
        assertThat(message.getQueueName()).isEqualTo(CrawlJobQueueName.API_JOBS);
        assertThat(message.getPayloadJson()).contains("crawlJobId");
        assertThat(message.getStatus()).isEqualTo(QueueMessageStatus.READY);
        assertThat(message.getAvailableAt()).isEqualTo(now);
        assertThat(message.getRetryCount()).isEqualTo(2);
        assertThat(message.getLastError()).isEqualTo("timeout");
    }

    @Test
    @DisplayName("builder should default retryCount to zero")
    void builderShouldDefaultRetryCountToZero() {
        Instant now = Instant.parse("2026-03-16T14:00:00Z");

        PersistentQueueMessageEntity message = PersistentQueueMessageEntity.builder()
                .queueName(CrawlJobQueueName.STATIC_SCRAPE_JOBS)
                .payloadJson("{\"crawlJobId\":11}")
                .status(QueueMessageStatus.READY)
                .availableAt(now)
                .createdAt(now)
                .build();

        assertThat(message.getRetryCount()).isZero();
    }
}
