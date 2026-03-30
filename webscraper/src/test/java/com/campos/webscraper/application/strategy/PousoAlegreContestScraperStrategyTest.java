package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.enrichment.InconfidentesEditalPdfMetadataParser;
import com.campos.webscraper.application.enrichment.PousoAlegreContestPdfEnricher;
import com.campos.webscraper.application.normalizer.PousoAlegreContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.PousoAlegreConcursosParser;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PousoAlegreContestScraperStrategy")
class PousoAlegreContestScraperStrategyTest {

    @Test
    @DisplayName("should support Pouso Alegre static municipal public contest sites")
    void shouldSupportPousoAlegreStaticMunicipalPublicContestSites() {
        PousoAlegreContestScraperStrategy strategy = new PousoAlegreContestScraperStrategy(
                new FakeJobFetcher(Map.of()),
                new PousoAlegreConcursosParser(),
                new PousoAlegreContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new PousoAlegreContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("municipal_pouso_alegre")
                .displayName("Prefeitura de Pouso Alegre - Concursos")
                .baseUrl("https://www.pousoalegre.mg.gov.br/concursos-publicos")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("pouso_alegre_html_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-30T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should scrape Pouso Alegre listing and detail pages into normalized contest postings")
    void shouldScrapePousoAlegreListingAndDetailPagesIntoNormalizedContestPostings() throws IOException {
        String listingUrl = "https://www.pousoalegre.mg.gov.br/concursos-publicos";
        String detailUrl = "https://www.pousoalegre.mg.gov.br/concursos_view/2314";
        String ignoredDetailUrl = "https://www.pousoalegre.mg.gov.br/concursos_view/2316";

        PousoAlegreContestScraperStrategy strategy = new PousoAlegreContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        listingUrl,
                        new FetchedPage(listingUrl, fixture("fixtures/pouso-alegre/pouso-alegre-concursos-listing.html"), 200,
                                "text/html", LocalDateTime.parse("2026-03-30T10:20:00")),
                        detailUrl,
                        new FetchedPage(detailUrl, fixture("fixtures/pouso-alegre/pouso-alegre-concurso-detail.html"), 200,
                                "text/html", LocalDateTime.parse("2026-03-30T10:20:01")),
                        ignoredDetailUrl,
                        new FetchedPage(ignoredDetailUrl, """
                                <html><body>
                                  <table>
                                    <tr><td>Tipo:</td><td>Chamamento Publico</td></tr>
                                    <tr><td>Súmula:</td><td>Edital de Chamamento Publico 01/2026</td></tr>
                                  </table>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:02"))
                )),
                new PousoAlegreConcursosParser(),
                new PousoAlegreContestPdfEnricher(pdfUrl -> """
                        Cargo: Analista de Sistemas
                        Escolaridade: ensino superior completo em Sistemas de Informacao
                        Inscricoes: de 10/04/2026 a 20/04/2026
                        A prova objetiva sera realizada em 30/05/2026.
                        ANEXO I - CRONOGRAMA
                        """, new InconfidentesEditalPdfMetadataParser()),
                new PousoAlegreContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_pouso_alegre",
                listingUrl,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("municipal_pouso_alegre");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getExternalId()).isEqualTo("municipal_pouso_alegre:005-2026");
            assertThat(item.getContestName()).contains("Processo Seletivo Simplificado nº 005/2026");
            assertThat(item.getPositionTitle()).isEqualTo("Analista de Sistemas");
            assertThat(item.getState()).isEqualTo("MG");
            assertThat(item.getPublishedAt()).isEqualTo(LocalDate.parse("2026-01-14"));
            assertThat(item.getRegistrationStartDate()).isEqualTo(LocalDate.parse("2026-04-10"));
            assertThat(item.getRegistrationEndDate()).isEqualTo(LocalDate.parse("2026-04-20"));
            assertThat(item.getExamDate()).isEqualTo(LocalDate.parse("2026-05-30"));
            assertThat(item.getContestStatus().name()).isEqualTo("OPEN");
            assertThat(item.getPayloadJson()).contains("pdfAnnexReferences");
        });
    }

    @Test
    @DisplayName("should derive stable id from numero ano metadata when title is generic")
    void shouldDeriveStableIdFromNumeroAnoMetadataWhenTitleIsGeneric() {
        String listingUrl = "https://www.pousoalegre.mg.gov.br/concursos-publicos";
        String detailUrl = "https://www.pousoalegre.mg.gov.br/concursos_view/2400";

        PousoAlegreContestScraperStrategy strategy = new PousoAlegreContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        listingUrl,
                        new FetchedPage(listingUrl, """
                                <html><body>
                                  <a href="/concursos_view/2400">Processo Seletivo</a>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:00")),
                        detailUrl,
                        new FetchedPage(detailUrl, """
                                <html><body>
                                  <table>
                                    <tr><td>Tipo:</td><td>Processo Seletivo</td></tr>
                                    <tr><td>Súmula:</td><td>Processo Seletivo</td></tr>
                                    <tr><td>Nº/ Ano:</td><td>007/2026</td></tr>
                                    <tr><td>Data:</td><td>20/02/2026</td></tr>
                                    <tr><td>Edital</td><td>Arquivo principal</td><td>20/02/2026</td><td><a href="/arquivo/ps-007-2026.pdf">PDF</a></td></tr>
                                  </table>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:01"))
                )),
                new PousoAlegreConcursosParser(),
                new PousoAlegreContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new PousoAlegreContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_pouso_alegre",
                listingUrl,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item ->
                assertThat(item.getExternalId()).isEqualTo("municipal_pouso_alegre:007-2026"));
    }

    @Test
    @DisplayName("should normalize fallback stable id from title or attachment labels when metadata row is absent")
    void shouldNormalizeFallbackStableIdFromTitleOrAttachmentLabelsWhenMetadataRowIsAbsent() {
        String listingUrl = "https://www.pousoalegre.mg.gov.br/concursos-publicos";
        String detailUrl = "https://www.pousoalegre.mg.gov.br/concursos_view/2401";

        PousoAlegreContestScraperStrategy strategy = new PousoAlegreContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        listingUrl,
                        new FetchedPage(listingUrl, """
                                <html><body>
                                  <a href="/concursos_view/2401">Processo Seletivo</a>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:00")),
                        detailUrl,
                        new FetchedPage(detailUrl, """
                                <html><body>
                                  <table>
                                    <tr><td>Tipo:</td><td>Processo Seletivo</td></tr>
                                    <tr><td>Súmula:</td><td>Processo Seletivo 5/2026</td></tr>
                                    <tr><td>Data:</td><td>20/02/2026</td></tr>
                                    <tr><td>Edital</td><td>Edital 5/2026</td><td>20/02/2026</td><td><a href="/arquivo/ps-5-2026.pdf">PDF</a></td></tr>
                                  </table>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:01"))
                )),
                new PousoAlegreConcursosParser(),
                new PousoAlegreContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new PousoAlegreContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_pouso_alegre",
                listingUrl,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item ->
                assertThat(item.getExternalId()).isEqualTo("municipal_pouso_alegre:005-2026"));
    }

    @Test
    @DisplayName("should fail scrape when a detail page cannot be fetched")
    void shouldFailScrapeWhenADetailPageCannotBeFetched() {
        String listingUrl = "https://www.pousoalegre.mg.gov.br/concursos-publicos";
        String detailUrl = "https://www.pousoalegre.mg.gov.br/concursos_view/2402";

        PousoAlegreContestScraperStrategy strategy = new PousoAlegreContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        listingUrl,
                        new FetchedPage(listingUrl, """
                                <html><body>
                                  <a href="/concursos_view/2402">Processo Seletivo</a>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:00")),
                        detailUrl,
                        new FetchedPage(detailUrl, "", 503, "text/html", LocalDateTime.parse("2026-03-30T10:20:01"))
                )),
                new PousoAlegreConcursosParser(),
                new PousoAlegreContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new PousoAlegreContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_pouso_alegre",
                listingUrl,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("detail request failed with status 503");
        assertThat(result.errorMessage()).contains(detailUrl);
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = PousoAlegreContestScraperStrategyTest.class.getClassLoader()
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
