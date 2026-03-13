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
 * In-memory queue implementation used to validate producer/consumer contracts before real brokers.
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
    public Optional<EnqueuedCrawlJob> consume(CrawlJobQueueName queueName) {
        Objects.requireNonNull(queueName, "queueName must not be null");
        return Optional.ofNullable(queues.get(queueName).poll());
    }
}
