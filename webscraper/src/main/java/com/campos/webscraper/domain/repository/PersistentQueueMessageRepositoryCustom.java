package com.campos.webscraper.domain.repository;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;

import java.time.Instant;
import java.util.Optional;

public interface PersistentQueueMessageRepositoryCustom {

    Optional<PersistentQueueMessageEntity> claimNextReadyMessage(CrawlJobQueueName queueName, Instant availableAt);

    Optional<PersistentQueueMessageEntity> markDone(Long messageId, Instant updatedAt);

    Optional<PersistentQueueMessageEntity> scheduleRetry(
            Long messageId,
            String payloadJson,
            Instant nextAvailableAt,
            Instant updatedAt,
            String lastError
    );

    Optional<PersistentQueueMessageEntity> moveToDeadLetter(
            Long messageId,
            String payloadJson,
            Instant updatedAt,
            String lastError
    );
}
