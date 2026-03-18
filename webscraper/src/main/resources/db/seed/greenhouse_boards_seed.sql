-- =============================================================================
-- Seed: Greenhouse Job Boards — empresas brasileiras de tecnologia
--
-- Convenções:
--   site_code  : greenhouse_{empresa}
--   base_url   : https://boards-api.greenhouse.io/v1/boards/{empresa}/jobs
--   site_type  : TYPE_E  (API pública oficial — ADR001: API-first)
--   extraction_mode : API
--   job_category    : PRIVATE_SECTOR
--   legal_status    : APPROVED (API pública, sem restrição em robots.txt/ToS)
--   selector_bundle_version : n/a (fontes API não usam seletores CSS)
--   enabled    : true
-- =============================================================================

INSERT INTO target_sites (
    site_code, display_name, base_url,
    site_type, extraction_mode, job_category, legal_status,
    selector_bundle_version, enabled, created_at
) VALUES
    (
        'greenhouse_stone',
        'Stone Co.',
        'https://boards-api.greenhouse.io/v1/boards/stone/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    ),
    (
        'greenhouse_nubank',
        'Nubank',
        'https://boards-api.greenhouse.io/v1/boards/nubank/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    ),
    (
        'greenhouse_creditas',
        'Creditas',
        'https://boards-api.greenhouse.io/v1/boards/creditas/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    ),
    (
        'greenhouse_dock',
        'Dock',
        'https://boards-api.greenhouse.io/v1/boards/dock/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    ),
    (
        'greenhouse_cloudwalk',
        'CloudWalk',
        'https://boards-api.greenhouse.io/v1/boards/cloudwalk/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    ),
    (
        'greenhouse_loft',
        'Loft',
        'https://boards-api.greenhouse.io/v1/boards/loft/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    ),
    (
        'greenhouse_kavak',
        'Kavak',
        'https://boards-api.greenhouse.io/v1/boards/kavak/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    ),
    (
        'greenhouse_olist',
        'Olist',
        'https://boards-api.greenhouse.io/v1/boards/olist/jobs',
        'TYPE_E', 'API', 'PRIVATE_SECTOR', 'APPROVED', 'n/a', true, NOW()
    );

-- Cria um crawl_job para cada target_site inserido acima
INSERT INTO crawl_jobs (target_site_id, scheduled_at, job_category, created_at)
SELECT id, NOW(), 'PRIVATE_SECTOR', NOW()
FROM target_sites
WHERE site_code IN (
    'greenhouse_stone',
    'greenhouse_nubank',
    'greenhouse_creditas',
    'greenhouse_dock',
    'greenhouse_cloudwalk',
    'greenhouse_loft',
    'greenhouse_kavak',
    'greenhouse_olist'
);
