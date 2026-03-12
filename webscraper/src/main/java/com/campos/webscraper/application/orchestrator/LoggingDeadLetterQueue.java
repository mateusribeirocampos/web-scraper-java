package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.model.CrawlJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Temporary dead-letter sink that records the routing decision in logs.
 */
@Component
public class LoggingDeadLetterQueue implements DeadLetterQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDeadLetterQueue.class);

    @Override
    public void route(CrawlJobEntity crawlJob, String reason) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        LOGGER.warn("Routing crawl job id={} to dead letter: {}", crawlJob.getId(), reason);
    }
}
