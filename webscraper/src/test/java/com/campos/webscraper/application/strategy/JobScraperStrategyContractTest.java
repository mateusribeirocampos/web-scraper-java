package com.campos.webscraper.application.strategy;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for JobScraperStrategy.
 *
 * TDD RED: written before the strategy interface exists.
 */
@Tag("unit")
@DisplayName("JobScraperStrategy contract")
class JobScraperStrategyContractTest {

    @Nested
    @DisplayName("supports(TargetSiteEntity)")
    class SupportsContract {

        @Test
        @DisplayName("should resolve support using explicit site metadata")
        void shouldResolveSupportUsingExplicitSiteMetadata() {
            JobScraperStrategy<String> strategy = new FakeIndeedApiStrategy();

            TargetSiteEntity supportedSite = buildSite(
                    "indeed-br",
                    "https://br.indeed.com",
                    SiteType.TYPE_E,
                    ExtractionMode.API,
                    JobCategory.PRIVATE_SECTOR,
                    LegalStatus.APPROVED
            );

            assertThat(strategy.supports(supportedSite)).isTrue();
        }

        @Test
        @DisplayName("should not infer support from url alone when metadata does not match")
        void shouldNotInferSupportFromUrlAloneWhenMetadataDoesNotMatch() {
            JobScraperStrategy<String> strategy = new FakeIndeedApiStrategy();

            TargetSiteEntity unsupportedSite = buildSite(
                    "indeed-br",
                    "https://br.indeed.com",
                    SiteType.TYPE_A,
                    ExtractionMode.STATIC_HTML,
                    JobCategory.PRIVATE_SECTOR,
                    LegalStatus.APPROVED
            );

            assertThat(strategy.supports(unsupportedSite)).isFalse();
        }
    }

    @Nested
    @DisplayName("scrape(ScrapeCommand)")
    class ScrapeContract {

        @Test
        @DisplayName("should return successful ScrapeResult when extraction succeeds")
        void shouldReturnSuccessfulScrapeResultWhenExtractionSucceeds() {
            JobScraperStrategy<String> strategy = new FakeIndeedApiStrategy();

            ScrapeCommand command = new ScrapeCommand(
                    "indeed-br",
                    "https://br.indeed.com/jobs?q=java+junior",
                    ExtractionMode.API,
                    JobCategory.PRIVATE_SECTOR
            );

            ScrapeResult<String> result = strategy.scrape(command);

            assertThat(result.success()).isTrue();
            assertThat(result.source()).isEqualTo("indeed-br");
            assertThat(result.items()).containsExactly("job-1", "job-2");
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should encapsulate expected extraction failure in ScrapeResult")
        void shouldEncapsulateExpectedExtractionFailureInScrapeResult() {
            JobScraperStrategy<String> strategy = new FakeFailureStrategy();

            ScrapeCommand command = new ScrapeCommand(
                    "pci-concursos",
                    "https://www.pciconcursos.com.br/concursos/ti",
                    ExtractionMode.STATIC_HTML,
                    JobCategory.PUBLIC_CONTEST
            );

            ScrapeResult<String> result = strategy.scrape(command);

            assertThat(result.success()).isFalse();
            assertThat(result.items()).isEmpty();
            assertThat(result.source()).isEqualTo("pci-concursos");
            assertThat(result.errorMessage()).contains("selector drift");
        }
    }

    private static TargetSiteEntity buildSite(
            String siteCode,
            String baseUrl,
            SiteType siteType,
            ExtractionMode extractionMode,
            JobCategory jobCategory,
            LegalStatus legalStatus
    ) {
        return TargetSiteEntity.builder()
                .siteCode(siteCode)
                .displayName("Test Site")
                .baseUrl(baseUrl)
                .siteType(siteType)
                .extractionMode(extractionMode)
                .jobCategory(jobCategory)
                .legalStatus(legalStatus)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-12T14:30:00Z"))
                .build();
    }

    private static final class FakeIndeedApiStrategy implements JobScraperStrategy<String> {

        @Override
        public boolean supports(TargetSiteEntity targetSite) {
            return "indeed-br".equals(targetSite.getSiteCode())
                    && targetSite.getSiteType() == SiteType.TYPE_E
                    && targetSite.getExtractionMode() == ExtractionMode.API
                    && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR
                    && targetSite.getLegalStatus() == LegalStatus.APPROVED;
        }

        @Override
        public ScrapeResult<String> scrape(ScrapeCommand command) {
            return ScrapeResult.success(List.of("job-1", "job-2"), command.siteCode());
        }
    }

    private static final class FakeFailureStrategy implements JobScraperStrategy<String> {

        @Override
        public boolean supports(TargetSiteEntity targetSite) {
            return "pci-concursos".equals(targetSite.getSiteCode());
        }

        @Override
        public ScrapeResult<String> scrape(ScrapeCommand command) {
            return ScrapeResult.failure(command.siteCode(), "selector drift on listing page");
        }
    }
}
