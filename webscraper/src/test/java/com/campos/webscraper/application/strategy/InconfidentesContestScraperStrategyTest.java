package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.enrichment.InconfidentesContestPdfEnricher;
import com.campos.webscraper.application.enrichment.InconfidentesEditalPdfMetadataParser;
import com.campos.webscraper.application.normalizer.InconfidentesContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.InconfidentesEditaisFixtureParser;
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
@DisplayName("InconfidentesContestScraperStrategy")
class InconfidentesContestScraperStrategyTest {

    @Test
    @DisplayName("should support Inconfidentes static municipal public contest sites")
    void shouldSupportInconfidentesStaticMunicipalPublicContestSites() {
        InconfidentesContestScraperStrategy strategy = new InconfidentesContestScraperStrategy(
                new FakeJobFetcher(Map.of()),
                new InconfidentesEditaisFixtureParser(),
                new InconfidentesContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new InconfidentesContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("municipal_inconfidentes")
                .displayName("Prefeitura de Inconfidentes - Editais")
                .baseUrl("https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("inconfidentes_html_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-25T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should scrape Inconfidentes fixture and return normalized municipal contest postings")
    void shouldScrapeInconfidentesFixtureAndReturnNormalizedMunicipalContestPostings() throws IOException {
        String url = "https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos";
        InconfidentesContestScraperStrategy strategy = new InconfidentesContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        url,
                        new FetchedPage(url, fixture("fixtures/inconfidentes/inconfidentes-editais-listing.html"), 200,
                                "text/html", LocalDateTime.parse("2026-03-25T10:20:00"))
                )),
                new InconfidentesEditaisFixtureParser(),
                new InconfidentesContestPdfEnricher(pdfUrl -> """
                        Cargo: Analista de Sistemas
                        Escolaridade: ensino superior completo em Sistemas de Informacao
                        Inscricoes: de 10/04/2026 a 20/04/2026
                        A prova objetiva sera realizada em 30/05/2026.
                        """, new InconfidentesEditalPdfMetadataParser()),
                new InconfidentesContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_inconfidentes",
                url,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("municipal_inconfidentes");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getContestName())
                    .isEqualTo("EDITAL 001/2026 - PROCESSO SELETIVO 001/2026 - CONTRATACAO DE PROFESSOR");
            assertThat(item.getPositionTitle()).isEqualTo("Analista de Sistemas");
            assertThat(item.getGovernmentLevel().name()).isEqualTo("MUNICIPAL");
            assertThat(item.getState()).isEqualTo("MG");
            assertThat(item.getRegistrationStartDate()).isEqualTo(java.time.LocalDate.parse("2026-04-10"));
            assertThat(item.getRegistrationEndDate()).isEqualTo(java.time.LocalDate.parse("2026-04-20"));
            assertThat(item.getExamDate()).isEqualTo(java.time.LocalDate.parse("2026-05-30"));
            assertThat(item.getContestStatus().name()).isEqualTo("OPEN");
        });
    }

    @Test
    @DisplayName("should ignore malformed blocks that do not expose a stable edital url")
    void shouldIgnoreMalformedBlocksThatDoNotExposeAStableEditalUrl() {
        String url = "https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos";
        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">EDITAL 006/2026 - PROCESSO SELETIVO 006/2026 - CONTRATACAO DE PROFESSOR</p>
                    <p><a href="https://example.com/resultado.pdf">Resultado Final</a></p>
                </div></body></html>
                """;

        InconfidentesContestScraperStrategy strategy = new InconfidentesContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        url,
                        new FetchedPage(url, html, 200, "text/html", LocalDateTime.parse("2026-03-25T10:20:00"))
                )),
                new InconfidentesEditaisFixtureParser(),
                new InconfidentesContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new InconfidentesContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_inconfidentes",
                url,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).isEmpty();
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = InconfidentesContestScraperStrategyTest.class.getClassLoader()
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
