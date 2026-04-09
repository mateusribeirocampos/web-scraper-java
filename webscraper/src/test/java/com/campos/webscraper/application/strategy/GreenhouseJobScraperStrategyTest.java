package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.GreenhouseJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.GreenhouseJobBoardClient;
import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
import com.campos.webscraper.interfaces.dto.GreenhouseLocationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("GreenhouseJobScraperStrategy")
class GreenhouseJobScraperStrategyTest {

    @Test
    @DisplayName("should support approved Bitso Greenhouse API sites using explicit metadata")
    void shouldSupportApprovedBitsoGreenhouseApiSitesUsingExplicitMetadata() {
        GreenhouseJobScraperStrategy strategy = new GreenhouseJobScraperStrategy(
                new FakeGreenhouseJobBoardClient(),
                new GreenhouseJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-13T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should integrate Greenhouse client and normalizer and return successful scrape result")
    void shouldIntegrateGreenhouseClientAndNormalizerAndReturnSuccessfulScrapeResult() {
        GreenhouseJobScraperStrategy strategy = new GreenhouseJobScraperStrategy(
                new FakeGreenhouseJobBoardClient(),
                new GreenhouseJobNormalizer()
        );

        ScrapeCommand command = new ScrapeCommand(
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        ScrapeResult<JobPostingEntity> result = strategy.scrape(command);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("greenhouse_bitso");
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).getExternalId()).isEqualTo("6120911003");
        assertThat(result.items().get(0).getCompany()).isEqualTo("Bitso");
        assertThat(result.items().get(0).getTechStackTags()).isEqualTo("Java");
        assertThat(result.items().get(0).isRemote()).isTrue();
    }

    private static final class FakeGreenhouseJobBoardClient extends GreenhouseJobBoardClient {

        @Override
        public List<GreenhouseJobBoardItemResponse> fetchPublishedJobs(String url) {
            return List.of(
                    new GreenhouseJobBoardItemResponse(
                            6120911003L,
                            "Senior Java Engineer",
                            "https://bitso.com/jobs/6120911003?gh_jid=6120911003",
                            "Bitso",
                            new GreenhouseLocationResponse("Latin America", "Remote"),
                            "2024-09-13T11:35:49-04:00",
                            "<p>Join Bitso to build reliable Java services for crypto and payments.</p>"
                    ),
                    new GreenhouseJobBoardItemResponse(
                            7655700003L,
                            "Senior Security Operations (SecOps) Engineer",
                            "https://bitso.com/jobs/7655700003?gh_jid=7655700003",
                            "Bitso",
                            new GreenhouseLocationResponse("Latin America", null),
                            "2026-03-06T11:09:26-05:00",
                            "<p>Security engineering role for cloud and incident response operations.</p>"
                    )
            );
        }
    }
}
