---
name: codebase-review-question-audit
description: Perform a deep structured review of the codebase, identify ambiguities, risks, and missing decisions, and generate a QUESTIONS.md file to clarify architecture, behavior, security, performance, and refactoring concerns before implementation.
version: 1.1.0
phase: discovery
produces: QUESTIONS.md
next: questions-md-resolution-implementation
project: web-scraper-java (Java 21 / Spring Boot 4 / PostgreSQL)
---

# Codebase Review Question Audit

## Purpose

Use this skill to perform a deep, structured review of the project before implementation or refactoring begins.

The purpose is to understand the codebase holistically and generate a `QUESTIONS.md` file containing all relevant technical, architectural, behavioral, product, security, and maintainability questions that should be answered before making broad changes.

This skill is designed for discovery, not implementation.

---

## Position in the workflow

This is **Phase 1 — Discovery** of the review-to-release workflow.

### Inputs
Access to the codebase and project documentation (CLAUDE.md, ADRs).

### Produces
- `QUESTIONS.md`

### Recommended next skill
- `questions-md-resolution-implementation`

### Do not proceed automatically if
- `QUESTIONS.md` still needs human answers
- major ambiguities remain unresolved
- the user wants to review the questions first

---

## When to use

Use this skill when the user asks to:

- review the whole codebase
- identify odd patterns, unclear decisions, or risks
- prepare questions before refactoring
- understand the architecture before making changes
- perform a discovery pass as a tech lead or staff engineer

Do not use this skill when the user explicitly wants direct implementation without a discovery phase.

---

## Core mindset

Act as:

- a professional code reviewer
- a staff/principal engineer
- a tech lead performing technical discovery on a Java / Spring Boot scraping platform

Your job is to understand first, question second, and change later.

If something looks unclear, risky, inconsistent, incomplete, or surprising, turn it into a question.

Do not assume intent when the code is ambiguous.

---

## Project-specific rules to verify (from CLAUDE.md)

These are hard constraints defined for this project. Every violation or ambiguity should become a question.

### TDD (INVIOLÁVEL)
- Every feature must have a failing test written **before** the production code.
- Cycle: test failing → minimum implementation → refactor → green.
- Questions to ask: Are there classes/methods with no corresponding test? Were tests added after production code (check git history)?

### API-first obrigatório
- Before any HTML scraper, there must be evidence that no official or public API exists.
- Tipo E (API) takes priority over Tipo A (HTML static), Tipo B (JS), Tipo C (browser automation).
- Questions to ask: Is there a scraper implemented where an API alternative exists?

### Legal checklist (ADR002)
- Sites classified as `SCRAPING_PROIBIDO` (LinkedIn, Catho, Glassdoor) must never be active.
- Every active scraper must have the legal checklist completed.
- Questions to ask: Are there `TargetSite` records or strategy implementations for forbidden sites?

### Domain model invariants
- `publishedAt: LocalDate` is mandatory in both `JobPostingEntity` and `PublicContestPostingEntity`.
- `fingerprintHash` (SHA-256 of siteCode + externalId + title + company + publishedAt) is mandatory for deduplication.
- Questions to ask: Are there missing `@NotNull` constraints on these fields? Is fingerprint calculation centralized?

### Strategy + Factory pattern
- Every site has a `JobScraperStrategy` implementation. `JobScraperFactory` resolves it.
- No site-specific logic should exist outside its strategy class.
- Questions to ask: Is there any routing logic leaked into orchestrators or controllers?

### CSS selectors — no hard-coding
- All CSS selectors must go through `SelectorBundle` (versioned).
- Questions to ask: Are there inline `String` selectors in any scraper code?

### Flyway migrations — immutable
- Applied migrations must never be edited.
- New schema changes must use a new version file.
- Questions to ask: Are there signs of edited applied migrations?

### Resilience baseline
- Every external HTTP call should be covered by at least Retry + RateLimiter (Resilience4j).
- Questions to ask: Are there HTTP clients not wrapped with a circuit breaker or rate limiter?

### Logging
- No `System.out.println` — all logging via SLF4J with `@Slf4j`.
- Questions to ask: Are there any `System.out` or `e.printStackTrace()` calls in the codebase?

### Configuration
- No hard-coded configuration values — use `application.properties`.
- Questions to ask: Are there URLs, credentials, or thresholds hard-coded in Java classes?

---

## Review scope

Review the project as broadly and deeply as possible, including:

**General**
- folder and module structure vs. declared architecture in CLAUDE.md
- framework conventions (Spring Boot 4, Java 21 virtual threads)
- dependency choices vs. approved stack
- environment variable and `application.properties` usage
- build assumptions (Maven wrapper, Spring Boot plugin)

**Domain & Application layer**
- domain model completeness (`JobPosting`, `PublicContestPosting`, enums, value objects)
- use case boundaries and orchestration logic
- normalizer responsibilities
- factory + strategy wiring

**Infrastructure**
- HTTP client usage (OkHttp vs. RestClient vs. Playwright)
- Playwright usage restricted to confirmed Tipo C sites only
- Flyway migration state and naming convention
- Scheduler configuration

**API / Interface layer**
- REST endpoint design (DTOs, response structure)
- missing validations at system boundaries

**Persistence**
- JPA entity mapping correctness
- index and constraint presence for `fingerprintHash` and `canonicalUrl`
- N+1 query risks

**Resilience**
- Resilience4j annotations or programmatic usage on external calls
- retry policies and idempotency of scraped operations

**Testing**
- TDD compliance signals (test vs. production code commit order)
- unit test coverage of domain invariants
- integration test coverage with Testcontainers
- WireMock usage for HTTP client tests
- missing test tags (`@Tag(“unit”)` / `@Tag(“integration”)`)

**Observability & Security**
- SLF4J usage, log levels, MDC correlation IDs
- no credentials or PII in logs
- no injection risks in dynamically built URLs or SQL

---

## Execution process

### 1. Understand the system first
Infer internally:

- what the project appears to do
- what the critical flows are (scrape → normalize → deduplicate → persist → expose)
- what the stack and architectural center are
- what seems mature versus unfinished (compare against ADR009 iteration plan)

### 2. Cross-check CLAUDE.md rules against the actual code
For each rule in the “Project-specific rules” section above, inspect the corresponding code and identify:
- violations
- partial implementations
- missing enforcement

### 3. Review the codebase systematically
Inspect the repository area by area and identify:

- ambiguity
- weak boundaries
- fragile logic
- missing invariants
- hidden assumptions
- missing validation
- missing tests
- security concerns
- product behavior ambiguity
- performance risks
- under-documented decisions

### 4. Convert findings into questions
Every relevant concern must be phrased as a question, not as a refactor prescription.

Good:
- “Is it intentional that `IndeedApiJobScraperStrategy` has no RateLimiter annotation?”

Avoid:
- “Add a RateLimiter to the strategy.”

### 5. Group questions by area
Organize questions into sections:

- Product & Intended Behavior
- Architecture & Layer Boundaries
- TDD Compliance
- Domain Model & Invariants
- Strategy / Factory Pattern Adherence
- API Design & DTOs
- Data & Persistence
- Resilience & Error Handling
- Legal & Compliance (ADR002)
- Security
- Performance
- Testing & QA
- Observability & Logging
- Configuration & Environment
- Flyway Migrations
- Technical Debt / Suspicious Areas
- Possible Bugs
- Missing Decisions / Open Design Gaps

### 6. Make each question independently answerable
Each question should be:

- specific
- contextualized
- self-contained
- easy to answer directly

Include when useful:

- file path and line number
- class or method name
- rule from CLAUDE.md that is at risk
- consequence if unanswered

### 7. Be exhaustive
Do not optimize for brevity. Optimize for completeness and clarity.

---

## Output

Create:

`QUESTIONS.md`

Suggested structure:

```
# QUESTIONS.md

## Project Understanding Summary
Brief summary of what the system appears to do, how it seems structured,
which ADR009 iterations appear complete, and what high-risk areas were identified.

## How to Answer
The project owner should answer each question and mark whether the item is:
- intended behavior
- bug
- approved improvement
- deferred
- out-of-scope

## Questions

### 1. TDD Compliance
#### Q1. ...
- **Where:** `path/to/File.java`
- **Rule:** TDD (CLAUDE.md)
- **Why this matters:** ...
- **Question:** ...

### 2. Architecture & Layer Boundaries
#### Q2. ...
...

Continue for all sections until all relevant questions are captured.

## Suggested answer tags
- `verified`
- `partial`
- `blocked`
- `deferred`
- `out-of-scope`
- `caveat`
```

---

## Quality bar

A strong `QUESTIONS.md` for this project:

- reveals TDD violations before they become habits
- surfaces any active scraper without legal clearance
- exposes domain invariants that are not enforced at the DB or JPA level
- identifies resilience gaps in external HTTP calls
- uncovers hard-coded selectors, credentials, or config values
- creates a real decision backlog aligned with ADR009 iterations

A weak `QUESTIONS.md` is shallow, generic, and misses the project-specific rules listed in CLAUDE.md.

---

## Constraints

Do not:

- change code automatically
- assume intended behavior silently
- jump into implementation
- collapse multiple concerns into vague notes
- skip the legal/compliance section — it is mandatory for this project

Do:

- inspect broadly
- question precisely
- anchor questions in evidence (file + line)
- cross-reference CLAUDE.md rules explicitly
- prefer clarity over politeness

---

## Handoff to next phase

This skill ends when `QUESTIONS.md` is complete.

### Recommended next step
Run `questions-md-resolution-implementation` **only after** the project owner has answered `QUESTIONS.md`.

### Stop condition
If `QUESTIONS.md` is unanswered or incomplete, do not continue to implementation.
