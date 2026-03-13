package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.model.CrawlJobEntity;

/**
 * Dispatches a crawl job to its execution path.
 */
public interface CrawlJobDispatcher {

    /**
     * Dispatches the given crawl job for execution.
     */
    CrawlExecutionStatus dispatch(CrawlJobEntity crawlJob);
}
