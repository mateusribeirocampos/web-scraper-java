INSERT INTO target_sites (
    site_code,
    display_name,
    base_url,
    site_type,
    extraction_mode,
    job_category,
    legal_status,
    selector_bundle_version,
    enabled,
    created_at,
    updated_at
)
SELECT
    'alcoa_pocos_caldas_workday',
    'Alcoa Careers via Workday - Poços de Caldas',
    'https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs',
    'TYPE_E',
    'API',
    'PRIVATE_SECTOR',
    'APPROVED',
    'n/a',
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM target_sites
    WHERE site_code = 'alcoa_pocos_caldas_workday'
);

UPDATE target_sites
SET display_name = 'Alcoa Careers via Workday - Poços de Caldas',
    base_url = 'https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs',
    site_type = 'TYPE_E',
    extraction_mode = 'API',
    job_category = 'PRIVATE_SECTOR',
    selector_bundle_version = 'n/a',
    legal_status = 'APPROVED',
    enabled = TRUE,
    updated_at = NOW()
WHERE site_code = 'alcoa_pocos_caldas_workday'
  AND legal_status <> 'SCRAPING_PROIBIDO';

INSERT INTO crawl_jobs (
    target_site_id,
    scheduled_at,
    job_category,
    created_at,
    scheduler_managed
)
SELECT
    ts.id,
    NOW(),
    'PRIVATE_SECTOR',
    NOW(),
    TRUE
FROM target_sites ts
WHERE ts.site_code = 'alcoa_pocos_caldas_workday'
  AND NOT EXISTS (
      SELECT 1
      FROM crawl_jobs cj
      WHERE cj.target_site_id = ts.id
        AND cj.scheduler_managed = TRUE
  );
