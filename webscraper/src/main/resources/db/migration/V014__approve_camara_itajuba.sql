UPDATE target_sites
SET legal_status = 'APPROVED',
    enabled = TRUE,
    updated_at = NOW()
WHERE site_code = 'camara_itajuba'
  AND legal_status <> 'SCRAPING_PROIBIDO';
