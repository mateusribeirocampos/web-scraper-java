package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.DouContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.DouApiClient;
import com.campos.webscraper.interfaces.dto.DouApiItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DouApiContestScraperStrategy.
 *
 * TDD RED: written before the strategy exists.
 */
@Tag("unit")
@DisplayName("DouApiContestScraperStrategy")
class DouApiContestScraperStrategyTest {

    @Test
    @DisplayName("should support DOU API sites using explicit metadata")
    void shouldSupportDouApiSitesUsingExplicitMetadata() {
        DouApiContestScraperStrategy strategy = new DouApiContestScraperStrategy(
                new FakeDouApiClient(),
                new DouContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("dou-api")
                .displayName("DOU API")
                .baseUrl("https://www.in.gov.br")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-12T14:30:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should integrate client and normalizer and return successful scrape result")
    void shouldIntegrateClientAndNormalizerAndReturnSuccessfulScrapeResult() {
        DouApiContestScraperStrategy strategy = new DouApiContestScraperStrategy(
                new FakeDouApiClient(),
                new DouContestNormalizer()
        );

        ScrapeCommand command = new ScrapeCommand(
                "dou-api",
                "https://www.in.gov.br/api/dou",
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(command);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("dou-api");
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).getExternalId()).isEqualTo("dou-1");
        assertThat(result.items().get(0).getGovernmentLevel().name()).isEqualTo("FEDERAL");
        assertThat(result.items().get(1).getExternalId()).isEqualTo("dou-2");
    }

    private static final class FakeDouApiClient extends DouApiClient {

        @Override
        public List<DouApiItemResponse> searchRelevantNotices(String url) {
            return List.of(
                    new DouApiItemResponse(
                            "dou-1",
                            "Analista de TI - Desenvolvimento de Sistemas",
                            "Concurso federal para analista de tecnologia da informacao",
                            "2026-03-10",
                            "https://www.in.gov.br/web/dou/-/edital-1"
                    ),
                    new DouApiItemResponse(
                            "dou-2",
                            "Desenvolvedor Backend Java",
                            "Processo seletivo com foco em tecnologia da informacao",
                            "2026-03-11",
                            "https://www.in.gov.br/web/dou/-/edital-2"
                    )
            );
        }
    }
}
