ALTER TABLE crawl_jobs
    ADD COLUMN scheduler_managed BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_crawl_jobs_scheduler_managed
    ON crawl_jobs (scheduler_managed, scheduled_at DESC);
