UPDATE target_sites
SET legal_status = 'APPROVED',
    enabled = TRUE
WHERE site_code = 'airbus_helibras_workday'
  AND legal_status <> 'SCRAPING_PROIBIDO';
