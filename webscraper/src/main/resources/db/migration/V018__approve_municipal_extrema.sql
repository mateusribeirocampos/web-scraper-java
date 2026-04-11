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
    'municipal_extrema',
    'Prefeitura de Extrema - Educação',
    'https://www.extrema.mg.gov.br/secretarias/educacao',
    'TYPE_A',
    'STATIC_HTML',
    'PUBLIC_CONTEST',
    'APPROVED',
    'extrema_html_v1',
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM target_sites
    WHERE site_code = 'municipal_extrema'
);

UPDATE target_sites
SET display_name = 'Prefeitura de Extrema - Educação',
    base_url = 'https://www.extrema.mg.gov.br/secretarias/educacao',
    site_type = 'TYPE_A',
    extraction_mode = 'STATIC_HTML',
    job_category = 'PUBLIC_CONTEST',
    selector_bundle_version = 'extrema_html_v1',
    legal_status = 'APPROVED',
    enabled = TRUE,
    updated_at = NOW()
WHERE site_code = 'municipal_extrema'
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
    'PUBLIC_CONTEST',
    NOW(),
    TRUE
FROM target_sites ts
WHERE ts.site_code = 'municipal_extrema'
  AND NOT EXISTS (
      SELECT 1
      FROM crawl_jobs cj
      WHERE cj.target_site_id = ts.id
        AND cj.scheduler_managed = TRUE
  );
