package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.enrichment.InconfidentesEditalPdfMetadataParser;
import com.campos.webscraper.application.enrichment.MunhozContestPdfEnricher;
import com.campos.webscraper.application.normalizer.MunhozContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.MunhozConcursosParser;
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
@DisplayName("MunhozContestScraperStrategy")
class MunhozContestScraperStrategyTest {

    @Test
    @DisplayName("should support Munhoz static municipal public contest sites")
    void shouldSupportMunhozStaticMunicipalPublicContestSites() {
        MunhozContestScraperStrategy strategy = new MunhozContestScraperStrategy(
                new FakeJobFetcher(Map.of()),
                new MunhozConcursosParser(),
                new MunhozContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new MunhozContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("municipal_munhoz")
                .displayName("Prefeitura de Munhoz - Concursos")
                .baseUrl("https://www.munhoz.mg.gov.br/concursos-publicos")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("munhoz_html_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-30T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should scrape Munhoz listing and detail pages into normalized contest postings")
    void shouldScrapeMunhozListingAndDetailPagesIntoNormalizedContestPostings() throws IOException {
        String listingUrl = "https://www.munhoz.mg.gov.br/concursos-publicos";
        String detailUrl = "https://www.munhoz.mg.gov.br/concursos_view/9";
        String ignoredDetailUrl = "https://www.munhoz.mg.gov.br/concursos_view/11";

        MunhozContestScraperStrategy strategy = new MunhozContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        listingUrl,
                        new FetchedPage(listingUrl, fixture("fixtures/munhoz/munhoz-concursos-listing.html"), 200,
                                "text/html", LocalDateTime.parse("2026-03-30T10:20:00")),
                        detailUrl,
                        new FetchedPage(detailUrl, fixture("fixtures/munhoz/munhoz-concurso-detail.html"), 200,
                                "text/html", LocalDateTime.parse("2026-03-30T10:20:01")),
                        ignoredDetailUrl,
                        new FetchedPage(ignoredDetailUrl, """
                                <html><body>
                                  <table>
                                    <tr><td>Tipo:</td><td>Chamamento Público</td></tr>
                                    <tr><td>Súmula:</td><td>Edital de Chamamento Público 01/2026</td></tr>
                                  </table>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:02"))
                )),
                new MunhozConcursosParser(),
                new MunhozContestPdfEnricher(pdfUrl -> """
                        Cargo: Docente de Educação Infantil
                        Escolaridade: ensino superior completo em Pedagogia
                        Inscricoes: de 01/04/2026 a 08/04/2026
                        A prova objetiva sera realizada em 27/04/2026.
                        ANEXO I - CRONOGRAMA
                        """, new InconfidentesEditalPdfMetadataParser()),
                new MunhozContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_munhoz",
                listingUrl,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getExternalId()).isEqualTo("municipal_munhoz:009-2026");
            assertThat(item.getContestName()).contains("PROCESSO SELETIVO SIMPLIFICADO Nº 009/2026");
            assertThat(item.getPositionTitle()).isEqualTo("Docente de Educação Infantil");
            assertThat(item.getPublishedAt()).isEqualTo(LocalDate.parse("2026-03-18"));
            assertThat(item.getRegistrationStartDate()).isEqualTo(LocalDate.parse("2026-04-01"));
            assertThat(item.getRegistrationEndDate()).isEqualTo(LocalDate.parse("2026-04-08"));
            assertThat(item.getExamDate()).isEqualTo(LocalDate.parse("2026-04-27"));
        });
    }

    @Test
    @DisplayName("should fail scrape when a detail page cannot be fetched")
    void shouldFailScrapeWhenADetailPageCannotBeFetched() {
        String listingUrl = "https://www.munhoz.mg.gov.br/concursos-publicos";
        String detailUrl = "https://www.munhoz.mg.gov.br/concursos_view/9";

        MunhozContestScraperStrategy strategy = new MunhozContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        listingUrl,
                        new FetchedPage(listingUrl, """
                                <html><body>
                                  <a href="/concursos_view/9">Processos Seletivos</a>
                                </body></html>
                                """, 200, "text/html", LocalDateTime.parse("2026-03-30T10:20:00")),
                        detailUrl,
                        new FetchedPage(detailUrl, "", 503, "text/html", LocalDateTime.parse("2026-03-30T10:20:01"))
                )),
                new MunhozConcursosParser(),
                new MunhozContestPdfEnricher(pdfUrl -> "", new InconfidentesEditalPdfMetadataParser()),
                new MunhozContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_munhoz",
                listingUrl,
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("detail request failed with status 503");
        assertThat(result.errorMessage()).contains(detailUrl);
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = MunhozContestScraperStrategyTest.class.getClassLoader()
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
