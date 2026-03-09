# ADR 008 - Observability, Security and Operational Governance for WebScraper

## Title

Observability baseline, security controls, and governance rules for scraping operations.

## Status

Proposed

## Date

2026-03-09

## Context

Scraping systems can create operational, legal, and reputational problems when they are not observable or governed. The project must balance engineering goals with controlled behavior toward external services.

## Decision

### 1. Observability Baseline

Every crawl execution must emit:

- correlation id,
- target site code,
- strategy name,
- parser version,
- execution duration,
- item count,
- failure category,
- retry count,
- rate-limit denials,
- queue latency.

Suggested metrics:

- `scrape_jobs_started_total`
- `scrape_jobs_succeeded_total`
- `scrape_jobs_failed_total`
- `scrape_records_extracted_total`
- `scrape_retry_total`
- `scrape_rate_limited_total`
- `scrape_dead_letter_total`
- `scrape_duration_seconds`

### 2. Security Controls

- Secrets stored outside source code.
- Authenticated target credentials isolated by site.
- Raw payload access restricted by role.
- Snapshot retention policy enforced.
- Outbound request headers standardized and reviewed.
- Browser contexts isolated between jobs.

### 3. Governance and Compliance Controls

Before enabling a site in production, record:

- business justification,
- target owner/team,
- robots.txt review result,
- terms-of-service review result,
- authentication approval status,
- rate-limit profile,
- escalation contact.

### 4. Operational Rules

- A scraper cannot be production-enabled without fixture tests.
- A site cannot be production-enabled with undefined rate limits.
- Browser scraping cannot be the default when static extraction is sufficient.
- Dead-letter queue items must generate actionable maintenance records.

### 5. Incident Categories

- selector drift,
- target layout rewrite,
- anti-bot or rate-limit escalation,
- authentication breakage,
- storage failure,
- duplicate explosion,
- compliance block.

### 6. TDD Link to Operations

Operational controls must also be test-driven where feasible:

- configuration validation tests,
- metrics emission tests,
- retention rule tests,
- site-enablement policy tests.

## Consequences

### Benefits

- Better debugging and maintenance prioritization.
- Reduced operational and reputational risk.
- More explainable scraper behavior in production.

### Challenges

- Governance adds overhead to onboarding new sites.
- Metrics without alerting thresholds can still be insufficient.

## Next Steps

1. Define dashboards and alert thresholds.
2. Add site-enablement checklist to the repository.
3. Add policy tests for production activation rules.
