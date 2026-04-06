UPDATE target_sites
SET legal_status = 'SCRAPING_PROIBIDO',
    enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE site_code = 'lever_watchguard';
