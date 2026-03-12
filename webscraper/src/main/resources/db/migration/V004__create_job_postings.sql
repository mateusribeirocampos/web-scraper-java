-- =============================================================================
-- V004: Job Postings
--
-- Private-sector job postings extracted from APIs or scrapers.
-- Relationship: CrawlExecution (1) -> JobPosting (N)
--               TargetSite (1) -> JobPosting (N)
-- =============================================================================

CREATE TABLE job_postings (
    id                    BIGSERIAL     PRIMARY KEY,
    crawl_execution_id    BIGINT        NOT NULL,
    target_site_id        BIGINT        NOT NULL,
    external_id           VARCHAR(255),
    canonical_url         VARCHAR(1000) NOT NULL,
    title                 VARCHAR(500)  NOT NULL,
    company               VARCHAR(300)  NOT NULL,
    location              VARCHAR(300),
    remote                BOOLEAN       NOT NULL,
    contract_type         VARCHAR(30)   NOT NULL,
    seniority             VARCHAR(20),
    salary_range          VARCHAR(500),
    tech_stack_tags       VARCHAR(1000),
    description           TEXT,
    published_at          DATE          NOT NULL,
    application_deadline  DATE,
    fingerprint_hash      VARCHAR(128)  NOT NULL,
    dedup_status          VARCHAR(20)   NOT NULL,
    payload_json          JSONB,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ,

    CONSTRAINT fk_job_postings_crawl_execution
        FOREIGN KEY (crawl_execution_id) REFERENCES crawl_executions (id),

    CONSTRAINT fk_job_postings_target_site
        FOREIGN KEY (target_site_id) REFERENCES target_sites (id)
);

CREATE INDEX idx_job_published_at
    ON job_postings (published_at DESC);

CREATE INDEX idx_job_site_external
    ON job_postings (target_site_id, external_id);

CREATE INDEX idx_job_fingerprint
    ON job_postings (fingerprint_hash);

CREATE INDEX idx_job_seniority_stack
    ON job_postings (seniority, tech_stack_tags);
