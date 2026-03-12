package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.domain.model.CrawlJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Temporary dispatcher that records scheduled triggers until the execution use case is implemented.
 */
@Component
public class LoggingCrawlJobDispatcher implements CrawlJobDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCrawlJobDispatcher.class);

    @Override
    public void dispatch(CrawlJobEntity crawlJob) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        LOGGER.info("Dispatching crawl job id={} siteCode={}",
                crawlJob.getId(),
                crawlJob.getTargetSite() != null ? crawlJob.getTargetSite().getSiteCode() : "unknown");
    }
}
