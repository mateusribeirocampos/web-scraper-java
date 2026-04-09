package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.PocosCaldasContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.PocosCaldasContestPdfParser;
import com.campos.webscraper.infrastructure.pdf.PdfTextExtractor;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PocosCaldasContestScraperStrategy")
class PocosCaldasContestScraperStrategyTest {

    @Test
    @DisplayName("should support approved Poços de Caldas PDF public contest source")
    void shouldSupportApprovedPocosDeCaldasPdfPublicContestSource() {
        PocosCaldasContestScraperStrategy strategy = new PocosCaldasContestScraperStrategy(
                pdfUrl -> "",
                new PocosCaldasContestPdfParser(),
                new PocosCaldasContestNormalizer(),
                request -> new FetchedPage(request.url(), "", 200, "text/html", LocalDateTime.now())
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("municipal_pocos_caldas")
                .displayName("Prefeitura de Poços de Caldas - Processo Seletivo Simplificado")
                .baseUrl("https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("pocos_caldas_pdf_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-09T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should discover canonical edital from contests listing and return successful scrape result")
    void shouldDiscoverCanonicalEditalFromContestsListingAndReturnSuccessfulScrapeResult() {
        PdfTextExtractor extractor = pdfUrl -> """
                EDITAL DE PROCESSO SELETIVO SIMPLIFICADO Nº 001/2025
                PROCESSO SELETIVO PÚBLICO SIMPLIFICADO PARA CONTRATAÇÃO DE PESSOAL POR PRAZO DETERMINADO PARA A PREFEITURA MUNICIPAL DE POÇOS DE CALDAS-MG
                4.1. Período: a partir das 10h do dia 01/09/2025 até às 16h do dia 09/09/2025.
                """;
        JobFetcher fetcher = request -> new FetchedPage(
                request.url(),
                """
                        <html><body>
                          <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/RETIFICACAO-001-2025.pdf">Retificação do Edital</a>
                          <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf">Edital de Processo Seletivo 001/2025</a>
                        </body></html>
                        """,
                200,
                "text/html",
                LocalDateTime.of(2026, 4, 9, 12, 0)
        );

        PocosCaldasContestScraperStrategy strategy = new PocosCaldasContestScraperStrategy(
                extractor,
                new PocosCaldasContestPdfParser(),
                new PocosCaldasContestNormalizer(),
                fetcher
        );

        var result = strategy.scrape(new ScrapeCommand(
                "municipal_pocos_caldas",
                "https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getExternalId()).isEqualTo("municipal_pocos_caldas:processo-seletivo-001-2025");
        assertThat(result.items().get(0).getCanonicalUrl())
                .isEqualTo("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf");
    }

    @Test
    @DisplayName("should pick higher same-year edital instead of first DOM match")
    void shouldPickHigherSameYearEditalInsteadOfFirstDomMatch() {
        PdfTextExtractor extractor = pdfUrl -> """
                EDITAL DE PROCESSO SELETIVO SIMPLIFICADO Nº 002/2026
                PROCESSO SELETIVO PÚBLICO SIMPLIFICADO PARA CONTRATAÇÃO DE PESSOAL
                4.1. Período: a partir das 10h do dia 01/02/2026 até às 16h do dia 09/02/2026.
                """;
        JobFetcher fetcher = request -> new FetchedPage(
                request.url(),
                """
                        <html><body>
                          <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/01/EDITAL-DE-PROCESSO-SELETIVO-001-2026.pdf">Edital 001/2026</a>
                          <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/02/EDITAL-DE-PROCESSO-SELETIVO-002-2026.pdf">Edital 002/2026</a>
                        </body></html>
                        """,
                200,
                "text/html",
                LocalDateTime.of(2026, 4, 9, 12, 0)
        );

        PocosCaldasContestScraperStrategy strategy = new PocosCaldasContestScraperStrategy(
                extractor,
                new PocosCaldasContestPdfParser(),
                new PocosCaldasContestNormalizer(),
                fetcher
        );

        var result = strategy.scrape(new ScrapeCommand(
                "municipal_pocos_caldas",
                "https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getCanonicalUrl())
                .isEqualTo("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/02/EDITAL-DE-PROCESSO-SELETIVO-002-2026.pdf");
    }
}
