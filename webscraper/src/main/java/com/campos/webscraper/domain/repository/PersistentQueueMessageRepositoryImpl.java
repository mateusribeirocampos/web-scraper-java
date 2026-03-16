package com.campos.webscraper.domain.repository;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
class PersistentQueueMessageRepositoryImpl implements PersistentQueueMessageRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Optional<PersistentQueueMessageEntity> claimNextReadyMessage(CrawlJobQueueName queueName, Instant availableAt) {
        @SuppressWarnings("unchecked")
        List<PersistentQueueMessageEntity> claimedMessages = entityManager.createNativeQuery("""
                WITH next_message AS (
                    SELECT id
                    FROM persistent_queue_messages
                    WHERE queue_name = :queueName
                      AND status = 'READY'
                      AND available_at <= :availableAt
                    ORDER BY available_at ASC, id ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                )
                UPDATE persistent_queue_messages queue_message
                SET status = 'CLAIMED',
                    claimed_at = :claimedAt,
                    updated_at = :claimedAt
                FROM next_message
                WHERE queue_message.id = next_message.id
                RETURNING queue_message.*
                """, PersistentQueueMessageEntity.class)
                .setParameter("queueName", queueName.name())
                .setParameter("availableAt", Timestamp.from(availableAt))
                .setParameter("claimedAt", Timestamp.from(availableAt))
                .getResultList();

        return claimedMessages.stream().findFirst();
    }
}
