package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.WorkdayJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.WorkdayJobBoardClient;
import com.campos.webscraper.interfaces.dto.WorkdayJobPostingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("WorkdayJobScraperStrategy")
class WorkdayJobScraperStrategyTest {

    @Test
    @DisplayName("should support approved Workday API private-sector sites using explicit metadata")
    void shouldSupportApprovedWorkdayApiPrivateSectorSitesUsingExplicitMetadata() {
        WorkdayJobScraperStrategy strategy = new WorkdayJobScraperStrategy(
                new FakeWorkdayJobBoardClient(),
                new WorkdayJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("airbus_helibras_workday")
                .displayName("Airbus / Helibras Careers via Workday")
                .baseUrl("https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-08T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should integrate Workday client and normalizer and return successful scrape result")
    void shouldIntegrateWorkdayClientAndNormalizerAndReturnSuccessfulScrapeResult() {
        WorkdayJobScraperStrategy strategy = new WorkdayJobScraperStrategy(
                new FakeWorkdayJobBoardClient(),
                new WorkdayJobNormalizer(
                        new ObjectMapper(),
                        Clock.fixed(Instant.parse("2026-04-08T12:00:00Z"), ZoneOffset.UTC)
                )
        );

        ScrapeCommand command = new ScrapeCommand(
                "airbus_helibras_workday",
                "https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        ScrapeResult<JobPostingEntity> result = strategy.scrape(command);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("airbus_helibras_workday");
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).getExternalId()).isEqualTo("JR10397592");
        assertThat(result.items().get(0).getCompany()).isEqualTo("Helibras / Airbus");
        assertThat(result.items().get(0).getPublishedAt().toString()).isEqualTo("2026-04-08");
        assertThat(result.items().get(1).getTechStackTags()).isEqualTo("SAP");
    }

    private static final class FakeWorkdayJobBoardClient extends WorkdayJobBoardClient {
        @Override
        public List<WorkdayJobPostingResponse> fetchJobsByLocation(String apiUrl, String locationFacetId) {
            return List.of(
                    new WorkdayJobPostingResponse(
                            "Estágio Técnico em Produção",
                            "/job/Itajub/Estgio-Tcnico-em-Produo_JR10397592",
                            "Itajubá",
                            "Posted Today",
                            List.of("JR10397592")
                    ),
                    new WorkdayJobPostingResponse(
                            "Analista de Sistemas SAP - SR | Senior SAP System Analyst",
                            "/job/Itajub/Analista-de-Sistemas-SAP---SR---Senior-SAP-System-Analyst_JR10374076",
                            "2 Locations",
                            "Posted 6 Days Ago",
                            List.of("JR10374076")
                    )
            );
        }
    }
}
