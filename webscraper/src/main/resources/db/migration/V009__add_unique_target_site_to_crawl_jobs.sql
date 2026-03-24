WITH duplicate_scheduler_jobs AS (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY target_site_id
                   ORDER BY created_at ASC, id ASC
               ) AS row_num
        FROM crawl_jobs
        WHERE scheduler_managed = TRUE
    ) ranked
    WHERE ranked.row_num > 1
)
UPDATE crawl_jobs
SET scheduler_managed = FALSE
WHERE id IN (SELECT id FROM duplicate_scheduler_jobs);

CREATE UNIQUE INDEX uk_crawl_jobs_target_site_scheduler_managed_true
    ON crawl_jobs (target_site_id)
    WHERE scheduler_managed = TRUE;
