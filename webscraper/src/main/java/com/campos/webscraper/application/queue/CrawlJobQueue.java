package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.model.CrawlJobEntity;

import java.util.Optional;

/**
 * Minimal queue abstraction for producer/consumer orchestration of crawl jobs.
 */
public interface CrawlJobQueue {

    /**
     * Enqueues a crawl job into the requested queue.
     */
    EnqueuedCrawlJob enqueue(CrawlJobEntity crawlJob, CrawlJobQueueName queueName);

    /**
     * Re-enqueues a previously materialized message into the requested queue.
     */
    EnqueuedCrawlJob enqueue(EnqueuedCrawlJob crawlJob, CrawlJobQueueName queueName);

    /**
     * Consumes the next available crawl job from the requested queue, if any.
     */
    Optional<EnqueuedCrawlJob> consume(CrawlJobQueueName queueName);
}
