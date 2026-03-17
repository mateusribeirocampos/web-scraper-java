package com.campos.webscraper.application.strategy;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.DynamicJobListingParser;
import com.campos.webscraper.application.normalizer.DynamicJobNormalizer;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PlaywrightDynamicScraperStrategy")
class PlaywrightDynamicScraperStrategyTest {

    private static final TargetSiteEntity DYNAMIC_SITE = TargetSiteEntity.builder()
            .siteCode("dynamic-playwright")
            .extractionMode(ExtractionMode.BROWSER_AUTOMATION)
            .jobCategory(JobCategory.PRIVATE_SECTOR)
            .legalStatus(LegalStatus.APPROVED)
            .siteType(SiteType.TYPE_C)
            .build();

    private final DynamicJobListingParser parser = new DynamicJobListingParser();
    private final DynamicJobNormalizer normalizer = new DynamicJobNormalizer();

    @Test
    @DisplayName("supports only approved Type C sites")
    void supportsOnlyDynamicSites() {
        PlaywrightDynamicScraperStrategy strategy = new PlaywrightDynamicScraperStrategy(
                request -> fakePage(200, "<body/>"),
                parser,
                normalizer
        );

        assertThat(strategy.supports(DYNAMIC_SITE)).isTrue();
    }

    @Test
    @DisplayName("returns failure when the Playwright fetch fails")
    void returnsFailureOnFetchError() {
        JobFetcher failingFetcher = request -> new FetchedPage(
                request.url(),
                "",
                503,
                null,
                LocalDateTime.now()
        );

        PlaywrightDynamicScraperStrategy strategy = new PlaywrightDynamicScraperStrategy(
                failingFetcher,
                parser,
                normalizer
        );

        ScrapeResult<JobPostingEntity> result = strategy.scrape(new ScrapeCommand(
                DYNAMIC_SITE.getSiteCode(),
                "https://dynamic.example.com",
                DYNAMIC_SITE.getExtractionMode(),
                DYNAMIC_SITE.getJobCategory()
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Playwright fetch failed");
    }

    @Test
    @DisplayName("parses dynamic job cards and returns success")
    void parsesDynamicJobCards() throws IOException {
        String html = Files.readString(Path.of("src/test/resources/fixtures/playwright/dynamic-site.html"));
        JobFetcher fetcher = request -> new FetchedPage(
                request.url(),
                html,
                200,
                "text/html",
                LocalDateTime.now()
        );

        PlaywrightDynamicScraperStrategy strategy = new PlaywrightDynamicScraperStrategy(
                fetcher,
                parser,
                normalizer
        );

        ScrapeResult<JobPostingEntity> result = strategy.scrape(new ScrapeCommand(
                DYNAMIC_SITE.getSiteCode(),
                "https://dynamic.example.com",
                DYNAMIC_SITE.getExtractionMode(),
                DYNAMIC_SITE.getJobCategory()
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).getTitle()).isEqualTo("Frontend Engineer");
        assertThat(result.items().get(1).getLocation()).isEqualTo("São Paulo");
    }

    private FetchedPage fakePage(int status, String html) {
        return new FetchedPage("https://dynamic.example.com", html, status, "text/html", LocalDateTime.now());
    }
}
