-- V010: raw HTTP snapshots captured during crawl executions
-- crawl_execution_id is nullable to allow orphan snapshots (e.g. smoke runs, pre-execution fetches)
CREATE TABLE raw_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    crawl_execution_id  BIGINT REFERENCES crawl_executions(id) ON DELETE SET NULL,
    site_code           VARCHAR(100) NOT NULL,
    fetched_at          TIMESTAMPTZ NOT NULL,
    response_body       TEXT NOT NULL,
    response_status     INT NOT NULL
);

CREATE INDEX idx_raw_snapshots_site_code         ON raw_snapshots (site_code);
CREATE INDEX idx_raw_snapshots_crawl_execution   ON raw_snapshots (crawl_execution_id);
CREATE INDEX idx_raw_snapshots_fetched_at        ON raw_snapshots (fetched_at DESC);
