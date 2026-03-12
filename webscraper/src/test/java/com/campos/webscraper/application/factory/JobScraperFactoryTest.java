package com.campos.webscraper.application.factory;

import com.campos.webscraper.application.strategy.JobScraperStrategy;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.shared.UnsupportedSiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JobScraperFactory resolution.
 *
 * TDD RED: written before the factory exists.
 */
@Tag("unit")
@DisplayName("JobScraperFactory")
class JobScraperFactoryTest {

    @Nested
    @DisplayName("resolve(TargetSiteEntity)")
    class ResolveTests {

        @Test
        @DisplayName("should resolve the first strategy that supports the target site")
        void shouldResolveTheFirstStrategyThatSupportsTheTargetSite() {
            JobScraperStrategy<String> expectedStrategy = new FakeIndeedApiStrategy();
            JobScraperFactory factory = new DefaultJobScraperFactory(List.of(
                    new FakeUnsupportedStrategy(),
                    expectedStrategy
            ));

            TargetSiteEntity site = buildSite(
                    "indeed-br",
                    SiteType.TYPE_E,
                    ExtractionMode.API,
                    JobCategory.PRIVATE_SECTOR
            );

            JobScraperStrategy<?> resolved = factory.resolve(site);

            assertThat(resolved).isSameAs(expectedStrategy);
        }

        @Test
        @DisplayName("should throw UnsupportedSiteException with descriptive metadata when no strategy supports the site")
        void shouldThrowUnsupportedSiteExceptionWithDescriptiveMetadata() {
            JobScraperFactory factory = new DefaultJobScraperFactory(List.of(
                    new FakeUnsupportedStrategy()
            ));

            TargetSiteEntity site = buildSite(
                    "dou-api",
                    SiteType.TYPE_E,
                    ExtractionMode.API,
                    JobCategory.PUBLIC_CONTEST
            );

            assertThatThrownBy(() -> factory.resolve(site))
                    .isInstanceOf(UnsupportedSiteException.class)
                    .hasMessageContaining("dou-api")
                    .hasMessageContaining("TYPE_E")
                    .hasMessageContaining("API")
                    .hasMessageContaining("PUBLIC_CONTEST");
        }

        @Test
        @DisplayName("should preserve registration order when more than one strategy reports support")
        void shouldPreserveRegistrationOrderWhenMoreThanOneStrategyReportsSupport() {
            JobScraperStrategy<String> first = new FakeCatchAllStrategy("first");
            JobScraperStrategy<String> second = new FakeCatchAllStrategy("second");
            JobScraperFactory factory = new DefaultJobScraperFactory(List.of(first, second));

            TargetSiteEntity site = buildSite(
                    "programaticamente",
                    SiteType.TYPE_A,
                    ExtractionMode.STATIC_HTML,
                    JobCategory.PRIVATE_SECTOR
            );

            JobScraperStrategy<?> resolved = factory.resolve(site);

            assertThat(resolved).isSameAs(first);
        }
    }

    private static TargetSiteEntity buildSite(
            String siteCode,
            SiteType siteType,
            ExtractionMode extractionMode,
            JobCategory jobCategory
    ) {
        return TargetSiteEntity.builder()
                .siteCode(siteCode)
                .displayName("Test Site")
                .baseUrl("https://example.com")
                .siteType(siteType)
                .extractionMode(extractionMode)
                .jobCategory(jobCategory)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-12T15:00:00Z"))
                .build();
    }

    private static final class FakeIndeedApiStrategy implements JobScraperStrategy<String> {

        @Override
        public boolean supports(TargetSiteEntity targetSite) {
            return "indeed-br".equals(targetSite.getSiteCode())
                    && targetSite.getSiteType() == SiteType.TYPE_E
                    && targetSite.getExtractionMode() == ExtractionMode.API
                    && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR;
        }

        @Override
        public ScrapeResult<String> scrape(ScrapeCommand command) {
            return ScrapeResult.success(List.of("job"), command.siteCode());
        }
    }

    private static final class FakeUnsupportedStrategy implements JobScraperStrategy<String> {

        @Override
        public boolean supports(TargetSiteEntity targetSite) {
            return false;
        }

        @Override
        public ScrapeResult<String> scrape(ScrapeCommand command) {
            return ScrapeResult.failure(command.siteCode(), "unsupported");
        }
    }

    private static final class FakeCatchAllStrategy implements JobScraperStrategy<String> {

        private final String sourceName;

        private FakeCatchAllStrategy(String sourceName) {
            this.sourceName = sourceName;
        }

        @Override
        public boolean supports(TargetSiteEntity targetSite) {
            return true;
        }

        @Override
        public ScrapeResult<String> scrape(ScrapeCommand command) {
            return ScrapeResult.success(List.of(sourceName), command.siteCode());
        }
    }
}
