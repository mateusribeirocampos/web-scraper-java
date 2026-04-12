package com.campos.webscraper.domain.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class AbstractRepositoryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected void truncateTables(String... tables) {
        jdbcTemplate.execute(
                "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE"
        );
    }

    protected void resetCrawlPersistence() {
        truncateTables("crawl_executions", "crawl_jobs", "target_sites");
    }

    protected void resetJobPostingPersistence() {
        truncateTables("job_postings", "crawl_executions", "crawl_jobs", "target_sites");
    }

    protected void resetPublicContestPersistence() {
        truncateTables("public_contest_postings", "crawl_executions", "crawl_jobs", "target_sites");
    }

    protected void resetRawSnapshots() {
        truncateTables("raw_snapshots");
    }

    protected void resetPersistentQueueMessages() {
        truncateTables("persistent_queue_messages");
    }
}
