package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.model.CrawlJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Temporary runner that keeps the lifecycle pipeline wired until source-specific execution is added.
 */
@Component
public class NoOpCrawlJobExecutionRunner implements CrawlJobExecutionRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpCrawlJobExecutionRunner.class);

    @Override
    public CrawlExecutionOutcome run(CrawlJobEntity crawlJob) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        LOGGER.info("Executing crawl job id={} with no-op runner", crawlJob.getId());
        return new CrawlExecutionOutcome(0, 0);
    }
}
