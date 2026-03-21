package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.GetScraperHealthSummaryUseCase;
import com.campos.webscraper.interfaces.dto.ScraperHealthSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Operational summary endpoint for scraper executions and queue state.
 */
@RestController
@RequestMapping("/api/v1/scraper")
public class ScraperHealthController {

    private final GetScraperHealthSummaryUseCase getScraperHealthSummaryUseCase;

    public ScraperHealthController(GetScraperHealthSummaryUseCase getScraperHealthSummaryUseCase) {
        this.getScraperHealthSummaryUseCase = Objects.requireNonNull(
                getScraperHealthSummaryUseCase,
                "getScraperHealthSummaryUseCase must not be null"
        );
    }

    @GetMapping("/health")
    public ScraperHealthSummaryResponse health() {
        return getScraperHealthSummaryUseCase.execute();
    }
}
