# ADR 007 - Testing, TDD and Quality Gates for WebScraper

## Title

Mandatory TDD workflow and quality gates for the WebScraper platform.

## Status

Accepted

## Date

2026-03-09

## Context

Scraping systems are fragile because external HTML, API payloads, and runtime behavior change frequently. The project therefore needs stronger-than-usual testing discipline. TDD is not optional here; it is the mechanism that protects extraction behavior from silent breakage.

## Decision

### 1. TDD Policy

**Every feature must be implemented with TDD.**

Required cycle:

1. Write a failing automated test.
2. Run the test and confirm failure for the expected reason.
3. Implement the smallest production change necessary.
4. Run tests and make them pass.
5. Refactor while keeping all tests green.

This policy applies to:

- scraper factory resolution,
- selector parsing,
- HTML fixture extraction,
- browser extraction,
- normalization,
- deduplication,
- JPA mappings,
- queue consumers,
- retry and rate limiting,
- REST endpoints,
- observability adapters.

### 2. Test Layers

#### Unit tests

Validate:

- selector extraction rules,
- normalizers,
- strategy support logic,
- deduplication functions,
- retry classification logic.

#### Fixture-based parser tests

Validate:

- real saved HTML snippets,
- parser version compatibility,
- missing-field behavior,
- selector drift detection.

#### Integration tests

Validate:

- repository behavior,
- application use cases,
- queue interactions,
- end-to-end job execution slices.

#### Contract and adapter tests

Validate:

- external HTTP client behavior against WireMock or equivalent mocked transport server,
- browser automation adapter contracts,
- serialization and persistence boundaries.

#### Manual user acceptance tests

Validate:

- that a configured source can actually be executed by the running application,
- that the resulting records become visible in the query endpoints,
- that the returned items still match a user-recognizable search intent.

Mandatory examples for acceptance rehearsal:

- private-sector jobs: `desenvolvedor de software em java spring boot`
- public contests: `concurso analista de ti` or `concurso desenvolvedor java`

Important scope note:

- when the platform still does not expose free-text search as a public input, the acceptance test
  must be exercised through a configured `CrawlJob` or source URL that encodes the same intent;
- once user-defined search input is implemented, this same scenario becomes an end-to-end product
  acceptance test rather than only an operator acceptance test.

#### Current user test flow in the project

As of 2026-03-13, the practical acceptance flow for the user is:

1. Prepare or reuse a `CrawlJob` whose configured source already represents the desired search
   intent.
2. Trigger manual execution with:
   - `POST /api/v1/crawl-jobs/{jobId}/execute`
3. Wait for the crawl execution lifecycle to finish successfully.
4. Query persisted records with:
   - `GET /api/v1/job-postings?since=YYYY-MM-DD&category=PRIVATE_SECTOR&seniority=...`
   - `GET /api/v1/public-contests?...`
5. Verify whether the returned items match the expected user intent, such as:
   - `desenvolvedor de software em java spring boot`
   - `concurso analista de ti`

This means the project already supports user-oriented acceptance verification, but the search
intent is still encoded in the configured source/job rather than accepted as arbitrary free text at
runtime.

### 3. Tooling

- JUnit 5
- Mockito
- AssertJ
- Testcontainers
- WireMock or equivalent mocked transport server
- JaCoCo

### 4. Quality Gates

A pull request must not be merged unless all of the following pass:

- unit tests,
- integration tests,
- static analysis,
- formatting/linting,
- coverage threshold,
- mutation or fixture stability checks where applicable.
- manual acceptance checklist updated when the story introduces or materially changes a source family.

### 5. Coverage Policy

A raw percentage alone is not enough, but the project should still enforce high coverage for critical layers:

- extractor strategies,
- normalizers,
- deduplication,
- resilience rules,
- persistence mappings.

### 6. Example Test Skeletons

```java
class ScraperFactoryTest {

    @Test
    void shouldResolveMarketplaceStrategyForMarketplaceSite() {
        // arrange
        // act
        // assert
    }
}
```

```java
class MarketplaceHtmlParserTest {

    @Test
    void shouldExtractTitlePriceAndDetailUrlFromFixture() {
        // given fixture html
        // when parse
        // then normalized values match expected output
    }
}
```

```java
class CrawlExecutionRepositoryIT {

    @Test
    void shouldPersistExecutionAndAssociatedExtractedRecords() {
        // Testcontainers + JPA integration
    }
}
```

## Consequences

### Benefits

- Lower risk of silent scraper regressions.
- Safer refactoring when selectors or models evolve.
- Better maintainability and team discipline.

### Challenges

- Initial implementation may feel slower.
- Fixture maintenance becomes part of the normal workflow.

## Next Steps

1. Create the initial test pyramid structure.
2. Add sample HTML fixtures for the first target site.
3. Block feature development unless the first failing tests exist.
4. Standardize a manual acceptance checklist for:
   - running a crawl job manually,
   - confirming execution success,
   - querying persisted results,
   - checking whether the returned items satisfy the expected user search intent.
