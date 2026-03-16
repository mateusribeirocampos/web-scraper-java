package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.enums.QueueMessageStatus;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.PersistentQueueMessageEntity;
import com.campos.webscraper.domain.repository.PersistentQueueMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Postgres-backed queue implementation that persists queue envelopes before worker adoption.
 */
@Component
@Primary
public class PersistentCrawlJobQueue implements CrawlJobQueue {

    private final PersistentQueueMessageRepository persistentQueueMessageRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PersistentCrawlJobQueue(PersistentQueueMessageRepository persistentQueueMessageRepository) {
        this(persistentQueueMessageRepository, new ObjectMapper().findAndRegisterModules(), Clock.systemUTC());
    }

    public PersistentCrawlJobQueue(
            PersistentQueueMessageRepository persistentQueueMessageRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.persistentQueueMessageRepository = Objects.requireNonNull(
                persistentQueueMessageRepository,
                "persistentQueueMessageRepository must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public EnqueuedCrawlJob enqueue(CrawlJobEntity crawlJob, CrawlJobQueueName queueName) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(queueName, "queueName must not be null");

        Instant now = Instant.now(clock);
        EnqueuedCrawlJob message = EnqueuedCrawlJob.from(crawlJob, queueName, now);
        persistentQueueMessageRepository.save(toPersistentMessage(message, queueName));
        return message;
    }

    @Override
    public EnqueuedCrawlJob enqueue(EnqueuedCrawlJob crawlJob, CrawlJobQueueName queueName) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(queueName, "queueName must not be null");

        EnqueuedCrawlJob message = crawlJob.withQueueName(queueName);
        persistentQueueMessageRepository.save(toPersistentMessage(message, queueName));
        return message;
    }

    @Override
    public Optional<EnqueuedCrawlJob> consume(CrawlJobQueueName queueName) {
        Objects.requireNonNull(queueName, "queueName must not be null");

        return persistentQueueMessageRepository.claimNextReadyMessage(queueName, Instant.now(clock))
                .map(this::deserializeMessage);
    }

    @Override
    public void markDone(EnqueuedCrawlJob crawlJob) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        if (crawlJob.queueMessageId() == null) {
            return;
        }
        persistentQueueMessageRepository.markDone(crawlJob.queueMessageId(), Instant.now(clock));
    }

    @Override
    public EnqueuedCrawlJob scheduleRetry(EnqueuedCrawlJob crawlJob, Instant nextAvailableAt, String lastError) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(nextAvailableAt, "nextAvailableAt must not be null");

        EnqueuedCrawlJob retriedMessage = crawlJob.forRetry(crawlJob.queueName(), nextAvailableAt);
        if (crawlJob.queueMessageId() == null) {
            persistentQueueMessageRepository.save(toPersistentMessage(retriedMessage, retriedMessage.queueName()));
            return retriedMessage;
        }
        persistentQueueMessageRepository.scheduleRetry(
                crawlJob.queueMessageId(),
                serializeMessage(retriedMessage),
                nextAvailableAt,
                Instant.now(clock),
                lastError
        );
        return retriedMessage;
    }

    @Override
    public EnqueuedCrawlJob moveToDeadLetter(EnqueuedCrawlJob crawlJob, String lastError) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");

        EnqueuedCrawlJob deadLetter = crawlJob.withQueueName(CrawlJobQueueName.DEAD_LETTER_JOBS);
        if (crawlJob.queueMessageId() == null) {
            persistentQueueMessageRepository.save(toPersistentMessage(deadLetter, CrawlJobQueueName.DEAD_LETTER_JOBS));
            return deadLetter;
        }
        persistentQueueMessageRepository.moveToDeadLetter(
                crawlJob.queueMessageId(),
                serializeMessage(deadLetter),
                Instant.now(clock),
                lastError
        );
        return deadLetter;
    }

    private PersistentQueueMessageEntity toPersistentMessage(EnqueuedCrawlJob message, CrawlJobQueueName queueName) {
        Instant now = Instant.now(clock);
        return PersistentQueueMessageEntity.builder()
                .queueName(queueName)
                .payloadJson(serializeMessage(message))
                .status(QueueMessageStatus.READY)
                .availableAt(message.availableAt())
                .retryCount(message.retryCount())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private String serializeMessage(EnqueuedCrawlJob message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize queue message", e);
        }
    }

    private EnqueuedCrawlJob deserializeMessage(PersistentQueueMessageEntity message) {
        try {
            return objectMapper.readValue(message.getPayloadJson(), EnqueuedCrawlJob.class)
                    .withQueueMessageId(message.getId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize queue message payload", e);
        }
    }
}
