package com.campos.webscraper.domain.enums;

/**
 * Lifecycle states for persisted queue messages used by async crawl handoff.
 */
public enum QueueMessageStatus {
    READY,
    CLAIMED,
    RETRY_WAIT,
    DEAD_LETTER,
    DONE
}
