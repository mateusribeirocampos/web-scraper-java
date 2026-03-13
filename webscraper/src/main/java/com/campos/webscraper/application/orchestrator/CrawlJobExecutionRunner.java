package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;

/**
 * Executes the actual work of a crawl job and returns counters for the execution log.
 */
public interface CrawlJobExecutionRunner {

    /**
     * Runs the crawl job and returns the resulting counters.
     */
    CrawlExecutionOutcome run(CrawlJobEntity crawlJob, CrawlExecutionEntity crawlExecution);
}
