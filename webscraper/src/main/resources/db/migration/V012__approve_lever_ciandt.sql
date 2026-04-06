UPDATE target_sites
SET legal_status = 'APPROVED',
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE site_code = 'lever_ciandt'
  AND legal_status <> 'SCRAPING_PROIBIDO';
