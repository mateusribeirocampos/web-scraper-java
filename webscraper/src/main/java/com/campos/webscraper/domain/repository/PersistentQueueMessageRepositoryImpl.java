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
                      AND status IN ('READY', 'RETRY_WAIT')
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

    @Override
    @Transactional
    public Optional<PersistentQueueMessageEntity> markDone(Long messageId, Instant updatedAt) {
        return updateClaimedMessage("""
                UPDATE persistent_queue_messages
                SET status = 'DONE',
                    claimed_at = NULL,
                    updated_at = :updatedAt
                WHERE id = :messageId
                  AND status = 'CLAIMED'
                RETURNING *
                """, messageId, updatedAt, null, null, null);
    }

    @Override
    @Transactional
    public Optional<PersistentQueueMessageEntity> scheduleRetry(
            Long messageId,
            String payloadJson,
            Instant nextAvailableAt,
            Instant updatedAt,
            String lastError
    ) {
        return updateClaimedMessage("""
                UPDATE persistent_queue_messages
                SET status = 'RETRY_WAIT',
                    payload_json = :payloadJson,
                    available_at = :availableAt,
                    retry_count = retry_count + 1,
                    last_error = :lastError,
                    claimed_at = NULL,
                    updated_at = :updatedAt
                WHERE id = :messageId
                  AND status = 'CLAIMED'
                RETURNING *
                """, messageId, updatedAt, nextAvailableAt, lastError, payloadJson);
    }

    @Override
    @Transactional
    public Optional<PersistentQueueMessageEntity> moveToDeadLetter(
            Long messageId,
            String payloadJson,
            Instant updatedAt,
            String lastError
    ) {
        return updateClaimedMessage("""
                UPDATE persistent_queue_messages
                SET status = 'DEAD_LETTER',
                    queue_name = 'DEAD_LETTER_JOBS',
                    payload_json = :payloadJson,
                    claimed_at = NULL,
                    last_error = :lastError,
                    updated_at = :updatedAt
                WHERE id = :messageId
                  AND status = 'CLAIMED'
                RETURNING *
                """, messageId, updatedAt, null, lastError, payloadJson);
    }

    private Optional<PersistentQueueMessageEntity> updateClaimedMessage(
            String sql,
            Long messageId,
            Instant updatedAt,
            Instant availableAt,
            String lastError,
            String payloadJson
    ) {
        var query = entityManager.createNativeQuery(sql, PersistentQueueMessageEntity.class)
                .setParameter("messageId", messageId)
                .setParameter("updatedAt", Timestamp.from(updatedAt));
        if (sql.contains(":availableAt")) {
            query.setParameter("availableAt", availableAt == null ? null : Timestamp.from(availableAt));
        }
        if (sql.contains(":lastError")) {
            query.setParameter("lastError", lastError);
        }
        if (sql.contains(":payloadJson")) {
            query.setParameter("payloadJson", payloadJson);
        }

        @SuppressWarnings("unchecked")
        List<PersistentQueueMessageEntity> updatedMessages = query.getResultList();

        return updatedMessages.stream().findFirst();
    }
}
