package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.IndeedJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.IndeedApiClient;
import com.campos.webscraper.interfaces.dto.IndeedApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IndeedApiJobScraperStrategy.
 *
 * TDD RED: written before the strategy exists.
 */
@Tag("unit")
@DisplayName("IndeedApiJobScraperStrategy")
class IndeedApiJobScraperStrategyTest {

    @Test
    @DisplayName("should support Indeed API sites using explicit metadata")
    void shouldSupportIndeedApiSitesUsingExplicitMetadata() {
        IndeedApiJobScraperStrategy strategy = new IndeedApiJobScraperStrategy(
                new FakeIndeedApiClient(),
                new IndeedJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("indeed-br")
                .displayName("Indeed Brasil")
                .baseUrl("https://br.indeed.com")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-12T14:20:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should integrate client and normalizer and return successful scrape result")
    void shouldIntegrateClientAndNormalizerAndReturnSuccessfulScrapeResult() {
        IndeedApiJobScraperStrategy strategy = new IndeedApiJobScraperStrategy(
                new FakeIndeedApiClient(),
                new IndeedJobNormalizer()
        );

        ScrapeCommand command = new ScrapeCommand(
                "indeed-br",
                "https://to.indeed.test/api/jobs/123",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        ScrapeResult<JobPostingEntity> result = strategy.scrape(command);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("indeed-br");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getExternalId()).isEqualTo("job-123");
        assertThat(result.items().get(0).getCompany()).isEqualTo("Invillia");
        assertThat(result.items().get(0).getTechStackTags()).isEqualTo("Java,Spring Boot");
    }

    private static final class FakeIndeedApiClient extends IndeedApiClient {

        @Override
        public IndeedApiResponse fetchJob(String url) {
            return new IndeedApiResponse(
                    "job-123",
                    "Java Backend Developer | Jr (Remote)",
                    "Invillia",
                    "Remoto",
                    "2026-03-05",
                    "https://to.indeed.com/job-123"
            );
        }
    }
}
