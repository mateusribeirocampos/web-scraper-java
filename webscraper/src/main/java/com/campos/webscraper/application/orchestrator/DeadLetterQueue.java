package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.model.CrawlJobEntity;

/**
 * Routes exhausted or blocked executions to dead-letter handling.
 */
public interface DeadLetterQueue {

    /**
     * Routes the crawl job to dead-letter handling with a reason.
     */
    void route(CrawlJobEntity crawlJob, String reason);
}
