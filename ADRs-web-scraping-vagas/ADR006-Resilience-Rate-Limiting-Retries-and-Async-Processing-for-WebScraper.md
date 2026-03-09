# ADR 006 - Resilience, Rate Limiting, Retries and Async Processing for WebScraper

## Title

Operational resilience design for external-site scraping workloads.

## Status

Proposed

## Date

2026-03-09

## Context

Scraping interacts with unreliable and rate-sensitive external systems. Timeouts, anti-bot responses, transient 5xx failures, and slow rendering are expected behavior. The system must fail safely and recover predictably.

## Decision

### 1. Resilience Model

The platform adopts the following resilience controls:

- **Retry** for transient network or upstream failures.
- **RateLimiter** per target site and sometimes per route family.
- **Bulkhead** to isolate heavy browser-driven jobs from lightweight HTML jobs.
- **CircuitBreaker** to stop hammering unstable sources.
- **Timeouts** for fetch, parse, and browser stages.

Resilience4j supports Retry, RateLimiter, Bulkhead and CircuitBreaker patterns with Spring Boot configuration. citeturn0search3turn0search7turn0search15turn0search23

### 2. Retry Rules

Retry only for transient cases such as:

- network timeout,
- connection reset,
- HTTP 429,
- HTTP 502/503/504,
- temporary browser startup failure.

Do **not** retry blindly for:

- selector not found,
- parsing rule mismatch,
- forbidden/legal disallow status,
- authentication failure due to invalid credentials,
- deterministic validation errors.

Suggested default:

- max attempts: 3
- exponential backoff with jitter
- site override allowed

### 3. Rate Limiting Rules

Each `TargetSite` must define:

- requests per minute,
- crawl delay between pages,
- browser-session concurrency limit,
- burst behavior policy.

There must be no “unlimited” profile in production.

### 4. Asynchronous Processing Model

#### Execution stages

1. Scheduler emits crawl command.
2. Command is enqueued.
3. Worker consumes command.
4. Worker resolves strategy and performs extraction.
5. Results are persisted.
6. Metrics and events are emitted.

#### Queue separation

- `static-scrape-jobs`
- `dynamic-browser-jobs`
- `reprocess-jobs`
- `dead-letter-jobs`

### 5. Failure Routing

- Recoverable transient failure → retry policy.
- Repeated transient failure exhaustion → dead-letter queue.
- Deterministic parser failure → mark execution as failed and open maintenance issue.
- Legal/compliance disallow → block execution immediately.

### 6. Example Spring Configuration Sketch

```yaml
resilience4j:
  retry:
    instances:
      siteFetch:
        maxAttempts: 3
        waitDuration: 2s
        enableExponentialBackoff: true
  ratelimiter:
    instances:
      siteFetch:
        limitForPeriod: 10
        limitRefreshPeriod: 60s
        timeoutDuration: 0
  bulkhead:
    instances:
      browserJobs:
        maxConcurrentCalls: 2
  circuitbreaker:
    instances:
      siteFetch:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        failureRateThreshold: 50
```

### 7. TDD Rule for Resilience Features

Before enabling each resilience control in production, write failing tests for:

- retryable vs non-retryable exceptions,
- rate-limiter denial behavior,
- dead-letter routing,
- async idempotency,
- circuit breaker open-state handling.

## Consequences

### Benefits

- More polite and stable interaction with external sources.
- Better containment of browser-heavy jobs.
- Lower chance of self-inflicted outages.

### Challenges

- Over-retrying can become abusive if misconfigured.
- Queue complexity increases operational overhead.
- Browser jobs require stricter resource isolation.

## Next Steps

1. Define default resilience profile templates.
2. Create queue abstractions and worker contracts.
3. Implement transient failure tests before production retry code.
