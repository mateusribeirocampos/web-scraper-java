# ADR 007 - Testing, TDD and Quality Gates for WebScraper

## Title

Mandatory TDD workflow and quality gates for the WebScraper platform.

## Status

Proposed

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

- external HTTP client behavior against WireMock,
- browser automation adapter contracts,
- serialization and persistence boundaries.

### 3. Tooling

- JUnit 5
- Mockito
- AssertJ
- Testcontainers
- WireMock
- JaCoCo

### 4. Quality Gates

A pull request must not be merged unless all of the following pass:

- unit tests,
- integration tests,
- static analysis,
- formatting/linting,
- coverage threshold,
- mutation or fixture stability checks where applicable.

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
