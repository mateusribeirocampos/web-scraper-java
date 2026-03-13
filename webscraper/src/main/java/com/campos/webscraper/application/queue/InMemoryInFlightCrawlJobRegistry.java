package com.campos.webscraper.application.queue;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory claim registry used to suppress duplicate scheduler enqueue in the same process lifetime.
 */
@Component
public class InMemoryInFlightCrawlJobRegistry implements InFlightCrawlJobRegistry {

    private final Set<Long> claimedJobIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryClaim(Long crawlJobId) {
        Objects.requireNonNull(crawlJobId, "crawlJobId must not be null");
        return claimedJobIds.add(crawlJobId);
    }

    @Override
    public void release(Long crawlJobId) {
        if (crawlJobId == null) {
            return;
        }
        claimedJobIds.remove(crawlJobId);
    }
}
