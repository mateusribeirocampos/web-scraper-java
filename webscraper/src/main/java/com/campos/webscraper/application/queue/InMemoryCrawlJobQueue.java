package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.model.CrawlJobEntity;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory queue implementation kept only for contract tests and local non-Spring scenarios.
 */
public class InMemoryCrawlJobQueue implements CrawlJobQueue {

    private final Map<CrawlJobQueueName, Queue<EnqueuedCrawlJob>> queues;
    private final Clock clock;

    public InMemoryCrawlJobQueue() {
        this(Clock.systemUTC());
    }

    public InMemoryCrawlJobQueue(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.queues = new EnumMap<>(CrawlJobQueueName.class);
        for (CrawlJobQueueName queueName : CrawlJobQueueName.values()) {
            queues.put(queueName, new ConcurrentLinkedQueue<>());
        }
    }

    @Override
    public EnqueuedCrawlJob enqueue(CrawlJobEntity crawlJob, CrawlJobQueueName queueName) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(queueName, "queueName must not be null");

        EnqueuedCrawlJob message = EnqueuedCrawlJob.from(crawlJob, queueName, Instant.now(clock));
        queues.get(queueName).add(message);
        return message;
    }

    @Override
    public EnqueuedCrawlJob enqueue(EnqueuedCrawlJob crawlJob, CrawlJobQueueName queueName) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(queueName, "queueName must not be null");

        EnqueuedCrawlJob message = crawlJob.withQueueName(queueName);
        queues.get(queueName).add(message);
        return message;
    }

    @Override
    public Optional<EnqueuedCrawlJob> consume(CrawlJobQueueName queueName) {
        Objects.requireNonNull(queueName, "queueName must not be null");
        Instant now = Instant.now(clock);
        Queue<EnqueuedCrawlJob> queue = queues.get(queueName);
        for (EnqueuedCrawlJob message : queue) {
            if (message.availableAt().isAfter(now)) {
                continue;
            }
            if (queue.remove(message)) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    @Override
    public void markDone(EnqueuedCrawlJob crawlJob) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
    }

    @Override
    public EnqueuedCrawlJob scheduleRetry(EnqueuedCrawlJob crawlJob, Instant nextAvailableAt, String lastError) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(nextAvailableAt, "nextAvailableAt must not be null");

        EnqueuedCrawlJob delayedRetry = crawlJob.forRetry(crawlJob.queueName(), nextAvailableAt);
        queues.get(delayedRetry.queueName()).add(delayedRetry);
        return delayedRetry;
    }

    @Override
    public EnqueuedCrawlJob moveToDeadLetter(EnqueuedCrawlJob crawlJob, String lastError) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");

        EnqueuedCrawlJob deadLetter = crawlJob.withQueueName(CrawlJobQueueName.DEAD_LETTER_JOBS);
        queues.get(CrawlJobQueueName.DEAD_LETTER_JOBS).add(deadLetter);
        return deadLetter;
    }
}
