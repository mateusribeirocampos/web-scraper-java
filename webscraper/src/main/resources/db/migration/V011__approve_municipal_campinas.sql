UPDATE target_sites
SET legal_status = 'APPROVED',
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE site_code = 'municipal_campinas'
  AND legal_status <> 'SCRAPING_PROIBIDO';
