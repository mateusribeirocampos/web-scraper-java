package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.LeverJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.LeverPostingsClient;
import com.campos.webscraper.interfaces.dto.LeverCategoriesResponse;
import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("LeverJobScraperStrategy")
class LeverJobScraperStrategyTest {

    @Test
    @DisplayName("should support approved CI&T Lever API sites using explicit metadata")
    void shouldSupportApprovedCiandtLeverApiSitesUsingExplicitMetadata() {
        LeverJobScraperStrategy strategy = new LeverJobScraperStrategy(
                new FakeLeverPostingsClient(),
                new LeverJobNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("lever_ciandt")
                .displayName("CI&T Careers via Lever")
                .baseUrl("https://api.lever.co/v0/postings/ciandt?mode=json")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-31T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should integrate Lever client and normalizer and return successful scrape result")
    void shouldIntegrateLeverClientAndNormalizerAndReturnSuccessfulScrapeResult() {
        LeverJobScraperStrategy strategy = new LeverJobScraperStrategy(
                new FakeLeverPostingsClient(),
                new LeverJobNormalizer()
        );

        ScrapeCommand command = new ScrapeCommand(
                "lever_ciandt",
                "https://api.lever.co/v0/postings/ciandt?mode=json",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        ScrapeResult<JobPostingEntity> result = strategy.scrape(command);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("lever_ciandt");
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).getExternalId()).isEqualTo("job-123");
        assertThat(result.items().get(0).getCompany()).isEqualTo("CI&T");
        assertThat(result.items().get(0).getTechStackTags()).isEqualTo("Java,AWS");
        assertThat(result.items().get(1).isRemote()).isTrue();
    }

    private static final class FakeLeverPostingsClient extends LeverPostingsClient {
        @Override
        public List<LeverPostingResponse> fetchPublishedJobs(String url) {
            return List.of(
                    new LeverPostingResponse(
                            "job-123",
                            "Senior Java Engineer",
                            "https://jobs.lever.co/ciandt/job-123",
                            "https://jobs.lever.co/ciandt/job-123/apply",
                            "onsite",
                            new LeverCategoriesResponse("Engineering", "Campinas, Brazil", "Full-time"),
                            "<p>Build Java services on AWS.</p>"
                    ),
                    new LeverPostingResponse(
                            "job-124",
                            "Frontend Engineer Remote",
                            "https://jobs.lever.co/ciandt/job-124",
                            "https://jobs.lever.co/ciandt/job-124/apply",
                            "remote",
                            new LeverCategoriesResponse("Engineering", "Remote - Brazil", "Full-time"),
                            "<p>Build frontends with React and TypeScript.</p>"
                    )
            );
        }
    }
}
