package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.CamaraItajubaContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.CamaraItajubaContestPageParser;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CamaraItajubaContestScraperStrategy")
class CamaraItajubaContestScraperStrategyTest {

    @Test
    @DisplayName("should support Itajubá Câmara static public contest sites")
    void shouldSupportItajubaCamaraStaticPublicContestSites() {
        CamaraItajubaContestScraperStrategy strategy = new CamaraItajubaContestScraperStrategy(
                new FakeJobFetcher(Map.of()),
                new CamaraItajubaContestPageParser(),
                new CamaraItajubaContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("camara_itajuba")
                .displayName("Câmara Municipal de Itajubá - Concurso Público")
                .baseUrl("https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("camara_itajuba_html_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-06T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should scrape Itajubá Câmara fixture and return normalized public contest postings")
    void shouldScrapeItajubaCamaraFixtureAndReturnNormalizedPublicContestPostings() throws Exception {
        String url = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        CamaraItajubaContestScraperStrategy strategy = new CamaraItajubaContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        url,
                        new FetchedPage(url, fixture("fixtures/camara-itajuba/camara-itajuba-launch-page.html"),
                                200, "text/html", LocalDateTime.parse("2026-04-06T10:00:00"))
                )),
                new CamaraItajubaContestPageParser(),
                new CamaraItajubaContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "camara_itajuba",
                url,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getGovernmentLevel().name()).isEqualTo("MUNICIPAL");
            assertThat(item.getState()).isEqualTo("MG");
            assertThat(item.getExternalId()).isEqualTo("camara_itajuba:concurso-publico-2023");
            assertThat(item.getContestStatus().name()).isEqualTo("OPEN");
        });
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = CamaraItajubaContestScraperStrategyTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class FakeJobFetcher implements JobFetcher {

        private final Map<String, FetchedPage> pagesByUrl;

        private FakeJobFetcher(Map<String, FetchedPage> pagesByUrl) {
            this.pagesByUrl = pagesByUrl;
        }

        @Override
        public FetchedPage fetch(FetchRequest request) {
            FetchedPage page = pagesByUrl.get(request.url());
            if (page == null) {
                throw new IllegalStateException("No fixture configured for url=" + request.url());
            }
            return page;
        }
    }
}
