package com.campos.webscraper.application.enrichment;

import com.campos.webscraper.infrastructure.parser.InconfidentesContestAttachment;
import com.campos.webscraper.infrastructure.parser.InconfidentesContestPreviewItem;
import com.campos.webscraper.infrastructure.pdf.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("InconfidentesContestPdfEnricher")
class InconfidentesContestPdfEnricherTest {

    @Test
    @DisplayName("should enrich preview item with metadata parsed from the edital pdf text")
    void shouldEnrichPreviewItemWithMetadataParsedFromTheEditalPdfText() {
        PdfTextExtractor extractor = pdfUrl -> """
                Cargo: Analista de Sistemas
                Escolaridade: ensino superior completo em Sistemas de Informacao
                Inscricoes: de 10/04/2026 a 20/04/2026
                """;
        InconfidentesContestPdfEnricher enricher = new InconfidentesContestPdfEnricher(
                extractor,
                new InconfidentesEditalPdfMetadataParser()
        );

        InconfidentesContestPreviewItem original = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 021/2026",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 021/2026",
                null,
                null,
                2026,
                "https://example.com/edital-021.pdf",
                "https://example.com/edital-021.pdf",
                null,
                null,
                null,
                List.of(new InconfidentesContestAttachment("Edital 021/2026", "https://example.com/edital-021.pdf")),
                List.of(),
                List.of()
        );

        InconfidentesContestPreviewItem enriched = enricher.enrich(original);

        assertThat(enriched.positionTitle()).isEqualTo("Analista de Sistemas");
        assertThat(enriched.educationLevel()).isEqualTo("SUPERIOR");
        assertThat(enriched.formationRequirements()).contains("ensino superior");
        assertThat(enriched.registrationStartDate()).isEqualTo(java.time.LocalDate.parse("2026-04-10"));
        assertThat(enriched.registrationEndDate()).isEqualTo(java.time.LocalDate.parse("2026-04-20"));
        assertThat(enriched.pdfPositionTitles()).containsExactly("Analista de Sistemas");
        assertThat(enriched.pdfAnnexReferences()).isEmpty();
    }

    @Test
    @DisplayName("should keep original preview item when pdf extraction fails")
    void shouldKeepOriginalPreviewItemWhenPdfExtractionFails() {
        PdfTextExtractor extractor = pdfUrl -> { throw new IllegalStateException("pdf fetch failed"); };
        InconfidentesContestPdfEnricher enricher = new InconfidentesContestPdfEnricher(
                extractor,
                new InconfidentesEditalPdfMetadataParser()
        );

        InconfidentesContestPreviewItem original = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 021/2026",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 021/2026",
                null,
                null,
                2026,
                "https://example.com/edital-021.pdf",
                "https://example.com/edital-021.pdf",
                null,
                null,
                null,
                List.of(new InconfidentesContestAttachment("Edital 021/2026", "https://example.com/edital-021.pdf")),
                List.of(),
                List.of()
        );

        InconfidentesContestPreviewItem enriched = enricher.enrich(original);

        assertThat(enriched).isEqualTo(original);
    }

    @Test
    @DisplayName("should stop enriching remaining items after the total pdf budget is exhausted")
    void shouldStopEnrichingRemainingItemsAfterTheTotalPdfBudgetIsExhausted() {
        AtomicInteger calls = new AtomicInteger();
        PdfTextExtractor extractor = pdfUrl -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return """
                    Cargo: Analista de Sistemas
                    Escolaridade: ensino superior completo em Sistemas de Informacao
                    Inscricoes: de 10/04/2026 a 20/04/2026
                    """;
        };
        InconfidentesContestPdfEnricher enricher = new InconfidentesContestPdfEnricher(
                extractor,
                new InconfidentesEditalPdfMetadataParser(),
                Duration.ofMillis(10)
        );

        InconfidentesContestPreviewItem first = buildPreviewItem("https://example.com/edital-1.pdf");
        InconfidentesContestPreviewItem second = buildPreviewItem("https://example.com/edital-2.pdf");

        List<InconfidentesContestPreviewItem> enriched = enricher.enrichAll(List.of(first, second));

        assertThat(calls.get()).isEqualTo(1);
        assertThat(enriched.get(0)).isEqualTo(first);
        assertThat(enriched.get(1)).isEqualTo(second);
    }

    @Test
    @DisplayName("should preserve html title when the pdf exposes multiple cargos")
    void shouldPreserveHtmlTitleWhenThePdfExposesMultipleCargos() {
        PdfTextExtractor extractor = pdfUrl -> """
                Cargo: Analista de Sistemas
                Cargo: Tecnico em Informatica
                Escolaridade: ensino superior completo em Sistemas de Informacao
                Inscricoes: de 10/04/2026 a 20/04/2026
                """;
        InconfidentesContestPdfEnricher enricher = new InconfidentesContestPdfEnricher(
                extractor,
                new InconfidentesEditalPdfMetadataParser()
        );

        InconfidentesContestPreviewItem original = buildPreviewItem("https://example.com/edital-021.pdf");

        InconfidentesContestPreviewItem enriched = enricher.enrich(original);

        assertThat(enriched.positionTitle()).isEqualTo("EDITAL 021/2026");
        assertThat(enriched.educationLevel()).isEqualTo("SUPERIOR");
    }

    @Test
    @DisplayName("should fall back to html preview when a single pdf extraction exceeds the remaining budget")
    void shouldFallBackToHtmlPreviewWhenASinglePdfExtractionExceedsTheRemainingBudget() throws Exception {
        CountDownLatch interrupted = new CountDownLatch(1);
        PdfTextExtractor extractor = pdfUrl -> {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException exception) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return "Cargo: Analista de Sistemas";
        };
        InconfidentesContestPdfEnricher enricher = new InconfidentesContestPdfEnricher(
                extractor,
                new InconfidentesEditalPdfMetadataParser(),
                Duration.ofMillis(50)
        );

        InconfidentesContestPreviewItem original = buildPreviewItem("https://example.com/edital-021.pdf");

        InconfidentesContestPreviewItem enriched = enricher.enrich(original, Duration.ofMillis(50));

        assertThat(enriched).isEqualTo(original);
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    private InconfidentesContestPreviewItem buildPreviewItem(String editalUrl) {
        return new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 021/2026",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 021/2026",
                null,
                null,
                2026,
                editalUrl,
                editalUrl,
                null,
                null,
                null,
                List.of(new InconfidentesContestAttachment("Edital 021/2026", editalUrl)),
                List.of(),
                List.of()
        );
    }

    @Test
    @DisplayName("should keep detailed pdf role and annex lists for downstream municipal payloads")
    void shouldKeepDetailedPdfRoleAndAnnexListsForDownstreamMunicipalPayloads() {
        PdfTextExtractor extractor = pdfUrl -> """
                Cargo: Analista de Sistemas
                Cargo: Tecnico em Informatica
                Escolaridade: ensino superior completo em Sistemas de Informacao
                ANEXO I - CRONOGRAMA
                ANEXO II - CONTEUDO PROGRAMATICO
                """;
        InconfidentesContestPdfEnricher enricher = new InconfidentesContestPdfEnricher(
                extractor,
                new InconfidentesEditalPdfMetadataParser()
        );

        InconfidentesContestPreviewItem enriched = enricher.enrich(buildPreviewItem("https://example.com/edital-021.pdf"));

        assertThat(enriched.positionTitle()).isEqualTo("EDITAL 021/2026");
        assertThat(enriched.pdfPositionTitles()).containsExactly("Analista de Sistemas", "Tecnico em Informatica");
        assertThat(enriched.pdfAnnexReferences())
                .containsExactly("ANEXO I - CRONOGRAMA", "ANEXO II - CONTEUDO PROGRAMATICO");
    }
}
