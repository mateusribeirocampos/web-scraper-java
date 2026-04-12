package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.GupyJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.GupyJobBoardClient;
import com.campos.webscraper.interfaces.dto.GupyJobListingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("GupyJobScraperStrategy")
class GupyJobScraperStrategyTest {

    @Test
    @DisplayName("should support approved Gupy API private-sector sites using gupy_ prefix and portal URL")
    void shouldSupportApprovedGupyApiPrivateSectorSitesUsingExplicitMetadata() {
        GupyJobScraperStrategy strategy = new GupyJobScraperStrategy(
                new FakeGupyJobBoardClient(),
                new GupyJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("gupy_specialdog_extrema")
                .displayName("Special Dog Company Careers via Gupy - Extrema")
                .baseUrl("https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-10T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should reject site with siteCode that does not start with gupy_")
    void shouldRejectSiteWithNonGupySiteCode() {
        GupyJobScraperStrategy strategy = new GupyJobScraperStrategy(
                new FakeGupyJobBoardClient(),
                new GupyJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("greenhouse_bitso")
                .baseUrl("https://portal.api.gupy.io/api/v1/jobs?careerPageName=Bitso")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .build();

        assertThat(strategy.supports(site)).isFalse();
    }

    @Test
    @DisplayName("should reject site whose baseUrl does not point to portal.api.gupy.io")
    void shouldRejectSiteWithNonGupyBaseUrl() {
        GupyJobScraperStrategy strategy = new GupyJobScraperStrategy(
                new FakeGupyJobBoardClient(),
                new GupyJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("gupy_other")
                .baseUrl("https://boards.greenhouse.io/specialdog")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .build();

        assertThat(strategy.supports(site)).isFalse();
    }

    @Test
    @DisplayName("should reject site with non-approved legal status")
    void shouldRejectSiteWithNonApprovedLegalStatus() {
        GupyJobScraperStrategy strategy = new GupyJobScraperStrategy(
                new FakeGupyJobBoardClient(),
                new GupyJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("gupy_specialdog_extrema")
                .baseUrl("https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .build();

        assertThat(strategy.supports(site)).isFalse();
    }

    @Test
    @DisplayName("should reject public contest site even with gupy_ prefix")
    void shouldRejectPublicContestSiteEvenWithGupyPrefix() {
        GupyJobScraperStrategy strategy = new GupyJobScraperStrategy(
                new FakeGupyJobBoardClient(),
                new GupyJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("gupy_prefeitura_campinas")
                .baseUrl("https://portal.api.gupy.io/api/v1/jobs?careerPageName=Prefeitura+Campinas")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .build();

        assertThat(strategy.supports(site)).isFalse();
    }

    @Test
    @DisplayName("should integrate Gupy client and normalizer and return successful scrape result for Special Dog board")
    void shouldIntegrateGupyClientAndNormalizerAndReturnSuccessfulScrapeResultForSpecialDogBoard() {
        GupyJobScraperStrategy strategy = new GupyJobScraperStrategy(
                new FakeGupyJobBoardClient(),
                new GupyJobNormalizer()
        );

        ScrapeCommand command = new ScrapeCommand(
                "gupy_specialdog_extrema",
                "https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        ScrapeResult<JobPostingEntity> result = strategy.scrape(command);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("gupy_specialdog_extrema");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getExternalId()).isEqualTo("98765");
        assertThat(result.items().get(0).getCompany()).isEqualTo("Special Dog Company");
        assertThat(result.items().get(0).getTitle()).isEqualTo("Desenvolvedor Java Júnior");
        assertThat(result.items().get(0).getPublishedAt().toString()).isEqualTo("2026-04-10");
    }

    private static final class FakeGupyJobBoardClient extends GupyJobBoardClient {
        @Override
        public List<GupyJobListingResponse> fetchAllJobs(String baseUrl) {
            return List.of(
                    new GupyJobListingResponse(
                            98765L,
                            "Desenvolvedor Java Júnior",
                            "Vaga de Java Júnior com Spring Boot",
                            "Special Dog Company",
                            "2026-04-10T00:00:00-03:00",
                            null,
                            false,
                            "Extrema",
                            "MG",
                            "Brasil",
                            "https://portal.gupy.io/job/98765",
                            "presential",
                            "full-time"
                    )
            );
        }
    }
}
