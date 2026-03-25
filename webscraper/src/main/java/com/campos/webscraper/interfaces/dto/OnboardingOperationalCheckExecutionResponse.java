package com.campos.webscraper.interfaces.dto;

import java.time.Instant;

public record OnboardingOperationalCheckExecutionResponse(
        Long crawlJobId,
        Long crawlExecutionId,
        String status,
        int itemsFound,
        Instant startedAt,
        Instant finishedAt
) {
}
