-- =============================================================================
-- V005: Public Contest Postings
--
-- Public contests extracted from official APIs or approved scraping sources.
-- Relationship: CrawlExecution (1) -> PublicContestPosting (N)
--               TargetSite (1) -> PublicContestPosting (N)
-- =============================================================================

CREATE TABLE public_contest_postings (
    id                       BIGSERIAL      PRIMARY KEY,
    crawl_execution_id       BIGINT         NOT NULL,
    target_site_id           BIGINT         NOT NULL,
    external_id              VARCHAR(255),
    canonical_url            VARCHAR(1000)  NOT NULL,
    contest_name             VARCHAR(500)   NOT NULL,
    organizer                VARCHAR(300)   NOT NULL,
    position_title           VARCHAR(300)   NOT NULL,
    government_level         VARCHAR(20)    NOT NULL,
    state                    VARCHAR(2),
    education_level          VARCHAR(20)    NOT NULL,
    number_of_vacancies      INTEGER,
    base_salary              NUMERIC(10,2),
    salary_description       VARCHAR(500),
    edital_url               VARCHAR(1000),
    published_at             DATE           NOT NULL,
    registration_start_date  DATE,
    registration_end_date    DATE,
    exam_date                DATE,
    contest_status           VARCHAR(30)    NOT NULL,
    fingerprint_hash         VARCHAR(128)   NOT NULL,
    dedup_status             VARCHAR(20)    NOT NULL,
    payload_json             JSONB,
    created_at               TIMESTAMPTZ    NOT NULL,
    updated_at               TIMESTAMPTZ,

    CONSTRAINT fk_public_contest_postings_crawl_execution
        FOREIGN KEY (crawl_execution_id) REFERENCES crawl_executions (id),

    CONSTRAINT fk_public_contest_postings_target_site
        FOREIGN KEY (target_site_id) REFERENCES target_sites (id)
);

CREATE INDEX idx_contest_published_at
    ON public_contest_postings (published_at DESC);

CREATE INDEX idx_contest_registration_end
    ON public_contest_postings (registration_end_date);

CREATE INDEX idx_contest_site_external
    ON public_contest_postings (target_site_id, external_id);

CREATE INDEX idx_contest_fingerprint
    ON public_contest_postings (fingerprint_hash);

CREATE INDEX idx_contest_gov_level_state
    ON public_contest_postings (government_level, state);
