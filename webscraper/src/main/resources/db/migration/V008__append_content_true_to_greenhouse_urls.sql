UPDATE target_sites
SET
    base_url = CASE
        WHEN position('?' in base_url) > 0 THEN base_url || '&content=true'
        ELSE base_url || '?content=true'
    END,
    updated_at = NOW()
WHERE site_code LIKE 'greenhouse_%'
  AND base_url NOT LIKE '%content=true%';
