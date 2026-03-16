package com.campos.webscraper.domain.repository;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for durable queue messages stored in Postgres.
 */
public interface PersistentQueueMessageRepository
        extends JpaRepository<PersistentQueueMessageEntity, Long>, PersistentQueueMessageRepositoryCustom {

    Optional<PersistentQueueMessageEntity> claimNextReadyMessage(CrawlJobQueueName queueName, Instant availableAt);
}
