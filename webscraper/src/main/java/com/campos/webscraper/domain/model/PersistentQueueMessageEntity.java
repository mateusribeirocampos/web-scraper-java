package com.campos.webscraper.domain.model;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Durable queue message for async crawl orchestration in Postgres.
 */
@Entity
@Table(
        name = "persistent_queue_messages",
        indexes = {
                @Index(name = "idx_queue_messages_status_available", columnList = "status, available_at"),
                @Index(name = "idx_queue_messages_queue_status", columnList = "queue_name, status, available_at")
        }
)
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PersistentQueueMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_name", nullable = false, length = 40)
    private CrawlJobQueueName queueName;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueMessageStatus status;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
