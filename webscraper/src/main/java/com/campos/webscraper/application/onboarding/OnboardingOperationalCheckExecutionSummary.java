package com.campos.webscraper.application.onboarding;

import java.time.Instant;

public record OnboardingOperationalCheckExecutionSummary(
        Long crawlJobId,
        Long crawlExecutionId,
        String status,
        int itemsFound,
        Instant startedAt,
        Instant finishedAt
) {
}
