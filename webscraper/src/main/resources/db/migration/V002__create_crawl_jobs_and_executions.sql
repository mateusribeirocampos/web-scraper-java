-- =============================================================================
-- V002: Crawl Jobs and Crawl Executions
--
-- CrawlJob   — scheduled scraping unit tied to a TargetSite.
-- CrawlExecution — single execution attempt of a CrawlJob, with lifecycle
--                  tracking (status, metrics, error, retry counter).
--
-- Relationship: TargetSite (1) → CrawlJob (N) → CrawlExecution (N)
-- =============================================================================

CREATE TABLE crawl_jobs (
    id              BIGSERIAL       PRIMARY KEY,
    target_site_id  BIGINT          NOT NULL,
    scheduled_at    TIMESTAMPTZ     NOT NULL,
    job_category    VARCHAR(30),
    created_at      TIMESTAMPTZ     NOT NULL,

    CONSTRAINT fk_crawl_jobs_target_site
        FOREIGN KEY (target_site_id) REFERENCES target_sites (id)
);

CREATE INDEX idx_crawl_jobs_target_site ON crawl_jobs (target_site_id);
CREATE INDEX idx_crawl_jobs_scheduled_at ON crawl_jobs (scheduled_at DESC);

-- =============================================================================

CREATE TABLE crawl_executions (
    id              BIGSERIAL       PRIMARY KEY,
    crawl_job_id    BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    pages_visited   INTEGER         NOT NULL DEFAULT 0,
    items_found     INTEGER         NOT NULL DEFAULT 0,
    retry_count     INTEGER         NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL,

    CONSTRAINT fk_crawl_executions_crawl_job
        FOREIGN KEY (crawl_job_id) REFERENCES crawl_jobs (id)
);

CREATE INDEX idx_crawl_executions_crawl_job ON crawl_executions (crawl_job_id);
CREATE INDEX idx_crawl_executions_status    ON crawl_executions (status);
CREATE INDEX idx_crawl_executions_started   ON crawl_executions (started_at DESC);
