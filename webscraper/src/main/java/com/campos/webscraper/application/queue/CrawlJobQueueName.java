package com.campos.webscraper.application.queue;

/**
 * Named queues used to segregate crawl workloads by execution profile.
 */
public enum CrawlJobQueueName {
    STATIC_SCRAPE_JOBS,
    API_JOBS,
    DYNAMIC_BROWSER_JOBS,
    DEAD_LETTER_JOBS
}
