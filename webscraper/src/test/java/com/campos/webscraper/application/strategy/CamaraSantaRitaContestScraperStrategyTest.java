package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.CamaraSantaRitaContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.CamaraSantaRitaProcessosSeletivosParser;
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
@DisplayName("CamaraSantaRitaContestScraperStrategy")
class CamaraSantaRitaContestScraperStrategyTest {

    @Test
    @DisplayName("should support Santa Rita Câmara static public contest sites")
    void shouldSupportSantaRitaCamaraStaticPublicContestSites() {
        CamaraSantaRitaContestScraperStrategy strategy = new CamaraSantaRitaContestScraperStrategy(
                new FakeJobFetcher(Map.of()),
                new CamaraSantaRitaProcessosSeletivosParser(),
                new CamaraSantaRitaContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("camara_santa_rita_sapucai")
                .displayName("Câmara Municipal de Santa Rita do Sapucaí - Processos Seletivos")
                .baseUrl("https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("camara_santa_rita_html_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-05T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should scrape Santa Rita Câmara fixture and return normalized public contest postings")
    void shouldScrapeSantaRitaCamaraFixtureAndReturnNormalizedPublicContestPostings() throws Exception {
        String url = "https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025";
        CamaraSantaRitaContestScraperStrategy strategy = new CamaraSantaRitaContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        url,
                        new FetchedPage(url, fixture("fixtures/camara-santa-rita/camara-santa-rita-processos-seletivos-2025.html"),
                                200, "text/html", LocalDateTime.parse("2026-04-05T10:00:00"))
                )),
                new CamaraSantaRitaProcessosSeletivosParser(),
                new CamaraSantaRitaContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "camara_santa_rita_sapucai",
                url,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.getGovernmentLevel().name()).isEqualTo("MUNICIPAL");
            assertThat(item.getState()).isEqualTo("MG");
            assertThat(item.getContestStatus().name()).isEqualTo("REGISTRATION_CLOSED");
        });
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = CamaraSantaRitaContestScraperStrategyTest.class.getClassLoader()
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
