# Executive Summary of WebScraper ADRs

This document provides an executive overview of the architectural decisions for the WebScraper platform.

## TDD Mandate

**All features in this project must be implemented with TDD as a non-negotiable delivery rule.**

That means every feature follows this sequence:

1. Write the failing test first.
2. Implement the minimum code necessary to make the test pass.
3. Refactor with tests green.
4. Promote the feature only when unit, integration, and contract checks are passing.

No scraper, parser, mapper, persistence adapter, scheduler, retry policy, rate-limiter rule, or anti-duplication rule may be considered complete without tests written before implementation.

## ADR Index

| ADR | Scope | Status |
|---|---|---|
| ADR001 | Architecture and technology direction | Proposed |
| ADR002 | Target-site taxonomy and requirements | Accepted |
| ADR003 | Java and Python scraping stack evaluation | Accepted |
| ADR004 | Extraction architecture with Strategy/Factory | Accepted |
| ADR005 | Persistence and JPA domain model | Accepted |
| ADR006 | Resilience, rate limiting, retries and async processing | Proposed |
| ADR007 | Testing, TDD and quality gates | Proposed |
| ADR008 | Observability, security and operational governance | Proposed |
| ADR009 | XP delivery plan and detailed tasks | Accepted |
| ADR010 | Open-source research and GitHub references | Accepted |

## Key Highlights

- Primary implementation language: **Java 21 + Spring Boot**.
- Primary static extraction library: **jsoup**, chosen for lightweight DOM parsing and selector-based extraction. 
- Dynamic extraction fallback: **Playwright for Java**, reserved for JS-heavy pages that cannot be handled reliably with plain HTTP + HTML parsing. 
- Python remains a benchmark/reference ecosystem because **Scrapy** provides a mature high-level crawling model and event-driven concurrency, but Java is selected for implementation consistency with the broader platform and career focus. 
- Resilience baseline: **Resilience4j** Retry, RateLimiter, Bulkhead, and CircuitBreaker patterns integrated with Spring Boot configuration. 
- Architecture style: layered backend with explicit extractor contracts, parser strategies, per-site factories, scheduler/orchestrator, queue-driven execution, and persistence separated from scraping logic.
- Delivery model: XP/TDD-first, with feature branches, acceptance criteria, red-green-refactor discipline, CI quality gates, and controlled rollout per scraper family.
- Current project state: Indeed, DOU, PCI parser/strategy, scheduler/manual execution, retry, rate limiting, circuit breaker, dead-letter, and legal onboarding validator are already implemented; queue-based async and health summary remain planned.

## Technical Compliance Snapshot

| Item | Current Direction |
|---|---|
| Java platform | Implemented |
| Scraper contracts | Implemented |
| JPA model | Implemented |
| TDD policy | Mandatory |
| Retry/rate limiting | Implemented |
| Circuit breaker/dead-letter | Implemented |
| Async processing | Planned |
| Observability | Planned |
| Security/robots governance | Partially implemented |
| Detailed XP tasks | Defined |

## Naming Convention

ADR files follow:

`ADR[3-digit-number]-[short-kebab-title].md`
