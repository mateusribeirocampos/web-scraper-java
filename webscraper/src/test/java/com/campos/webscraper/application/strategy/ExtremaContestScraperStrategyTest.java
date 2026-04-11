package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.enrichment.ExtremaContestPdfEnricher;
import com.campos.webscraper.application.normalizer.ExtremaContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.ExtremaConcursosParser;
import com.campos.webscraper.infrastructure.pdf.PdfTextExtractor;
import com.campos.webscraper.shared.FetchedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("ExtremaContestScraperStrategy")
class ExtremaContestScraperStrategyTest {

    @Test
    @DisplayName("should support approved Extrema municipal contest source")
    void shouldSupportApprovedExtremaMunicipalContestSource() {
        ExtremaContestScraperStrategy strategy = new ExtremaContestScraperStrategy(
                request -> new FetchedPage(request.url(), "", 200, "text/html", LocalDateTime.now()),
                new ExtremaConcursosParser(),
                new ExtremaContestPdfEnricher(pdfUrl -> "", new com.campos.webscraper.application.enrichment.InconfidentesEditalPdfMetadataParser()),
                new ExtremaContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("municipal_extrema")
                .displayName("Prefeitura de Extrema - Educação")
                .baseUrl("https://www.extrema.mg.gov.br/secretarias/educacao")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("extrema_html_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-10T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should discover official Extrema detail page and produce canonical contest posting")
    void shouldDiscoverOfficialExtremaDetailPageAndProduceCanonicalContestPosting() {
        PdfTextExtractor extractor = pdfUrl -> """
                EDITAL 003/2025
                INSCRIÇÕES: 18/07/2025 a 31/07/2025
                PROVA OBJETIVA: 10/08/2025
                Cargo: Professor de Educação Básica
                Escolaridade: Ensino Superior completo
                """;
        JobFetcher fetcher = request -> {
            if (request.url().equals("https://www.extrema.mg.gov.br/secretarias/educacao")) {
                return new FetchedPage(
                        request.url(),
                        """
                                <html><body>
                                  <a class="list-item__link" href="/secretarias/educacao/processo-seletivo-publico-simplificado-para-contratacao-de-cargosfuncoes-publicas-para-o-quadro-da-educacao-de-extrema">
                                    Processo seletivo público simplificado para contratação de cargos/funções públicas para o quadro da Educação de Extrema
                                  </a>
                                </body></html>
                                """,
                        200,
                        "text/html",
                        LocalDateTime.of(2026, 4, 10, 12, 0)
                );
            }
            return new FetchedPage(
                    request.url(),
                    """
                            <html>
                              <head><meta property="article:published_time" content="2025-07-17T20:08:37Z" /></head>
                              <body>
                                <main class="page-secretarias__grid-item conteudo">
                                  <h1 class="page-secretarias__titulo">Processo seletivo público simplificado para contratação de cargos/funções públicas para o quadro da Educação de Extrema</h1>
                                  <p><a href="https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_17072025170837.pdf">Edital nº003/2025</a></p>
                                  <p><a href="https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_12082025091208.pdf">Retificação do Edital nº 003/2025</a></p>
                                </main>
                              </body>
                            </html>
                            """,
                    200,
                    "text/html",
                    LocalDateTime.of(2026, 4, 10, 12, 0)
            );
        };

        ExtremaContestScraperStrategy strategy = new ExtremaContestScraperStrategy(
                fetcher,
                new ExtremaConcursosParser(),
                new ExtremaContestPdfEnricher(extractor, new com.campos.webscraper.application.enrichment.InconfidentesEditalPdfMetadataParser()),
                new ExtremaContestNormalizer()
        );

        var result = strategy.scrape(new ScrapeCommand(
                "municipal_extrema",
                "https://www.extrema.mg.gov.br/secretarias/educacao",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().getExternalId()).isEqualTo("municipal_extrema:003-2025");
        assertThat(result.items().getFirst().getCanonicalUrl())
                .isEqualTo("https://www.extrema.mg.gov.br/secretarias/educacao/processo-seletivo-publico-simplificado-para-contratacao-de-cargosfuncoes-publicas-para-o-quadro-da-educacao-de-extrema");
    }
}
