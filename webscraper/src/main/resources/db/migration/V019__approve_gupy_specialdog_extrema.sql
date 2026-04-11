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
    'gupy_specialdog_extrema',
    'Special Dog Company Careers via Gupy - Extrema',
    'https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema',
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
    WHERE site_code = 'gupy_specialdog_extrema'
);

UPDATE target_sites
SET display_name = 'Special Dog Company Careers via Gupy - Extrema',
    base_url = 'https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema',
    site_type = 'TYPE_E',
    extraction_mode = 'API',
    job_category = 'PRIVATE_SECTOR',
    selector_bundle_version = 'n/a',
    legal_status = 'APPROVED',
    enabled = TRUE,
    updated_at = NOW()
WHERE site_code = 'gupy_specialdog_extrema'
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
WHERE ts.site_code = 'gupy_specialdog_extrema'
  AND NOT EXISTS (
      SELECT 1
      FROM crawl_jobs cj
      WHERE cj.target_site_id = ts.id
        AND cj.scheduler_managed = TRUE
  );
