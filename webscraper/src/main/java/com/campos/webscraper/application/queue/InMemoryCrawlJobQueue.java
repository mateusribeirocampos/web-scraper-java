package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.model.CrawlJobEntity;
import org.springframework.stereotype.Component;

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
@Component
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
}
