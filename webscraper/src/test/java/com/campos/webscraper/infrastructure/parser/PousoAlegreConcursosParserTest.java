package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PousoAlegreConcursosParser")
class PousoAlegreConcursosParserTest {

    private final PousoAlegreConcursosParser parser = new PousoAlegreConcursosParser();

    @Test
    @DisplayName("should extract detail urls from concursos listing page")
    void shouldExtractDetailUrlsFromConcursosListingPage() throws IOException {
        String sourceUrl = "https://www.pousoalegre.mg.gov.br/concursos-publicos";

        assertThat(parser.parseListingUrls(fixture("fixtures/pouso-alegre/pouso-alegre-concursos-listing.html"), sourceUrl))
                .containsExactly(
                        "https://www.pousoalegre.mg.gov.br/concursos_view/2314",
                        "https://www.pousoalegre.mg.gov.br/concursos_view/2316"
                );
    }

    @Test
    @DisplayName("should filter follow-up listing rows before fetching detail pages")
    void shouldFilterFollowUpListingRowsBeforeFetchingDetailPages() {
        String html = """
                <html><body>
                  <table>
                    <tr class="text-center text-sm">
                      <td class="d-none">23</td>
                      <td class="d-none">2026</td>
                      <td>23/2026</td>
                      <td>Processo Seletivo</td>
                      <td>EDITAL DE CONVOCAÇÃO Processo Seletivo Simplificado nº 023/2026</td>
                      <td>24/03/2026</td>
                      <td><a href="/concursos_view/2337">ver</a></td>
                    </tr>
                    <tr class="text-center text-sm">
                      <td class="d-none">5</td>
                      <td class="d-none">2026</td>
                      <td>05/2026</td>
                      <td>Processo Seletivo</td>
                      <td>Processo Seletivo Simplificado nº 005/2026</td>
                      <td>14/01/2026</td>
                      <td><a href="/concursos_view/2314">ver</a></td>
                    </tr>
                  </table>
                </body></html>
                """;

        assertThat(parser.parseListingUrls(html, "https://www.pousoalegre.mg.gov.br/concursos-publicos"))
                .containsExactly("https://www.pousoalegre.mg.gov.br/concursos_view/2314");
    }

    @Test
    @DisplayName("should parse structured contest detail and select edital attachment")
    void shouldParseStructuredContestDetailAndSelectEditalAttachment() throws IOException {
        String sourceUrl = "https://www.pousoalegre.mg.gov.br/concursos_view/2314";

        PousoAlegreContestPreviewItem item = parser.parseDetail(
                fixture("fixtures/pouso-alegre/pouso-alegre-concurso-detail.html"),
                sourceUrl
        );

        assertThat(item).isNotNull();
        assertThat(item.contestTitle()).contains("Processo Seletivo Simplificado nº 005/2026");
        assertThat(item.organizer()).isEqualTo("Prefeitura Municipal de Pouso Alegre");
        assertThat(item.editalUrl()).isEqualTo("https://www.pousoalegre.mg.gov.br/arquivo/edital-005-2026.pdf");
        assertThat(item.contestUrl()).isEqualTo(sourceUrl);
        assertThat(item.contestNumber()).isEqualTo("005/2026");
        assertThat(item.publishedAt()).isEqualTo(java.time.LocalDate.parse("2026-01-14"));
        assertThat(item.editalYear()).isEqualTo(2026);
        assertThat(item.attachments())
                .extracting(PousoAlegreContestAttachment::type)
                .containsExactly("Classificação", "Edital", "CONVOCAÇÃO");
    }

    @Test
    @DisplayName("should ignore non operational detail pages like chamamento publico")
    void shouldIgnoreNonOperationalDetailPagesLikeChamamentoPublico() {
        String html = """
                <html><body>
                  <table>
                    <tr><td>Tipo:</td><td>Chamamento Publico</td></tr>
                    <tr><td>Sumula:</td><td>Edital de Chamamento Publico 01/2026</td></tr>
                  </table>
                </body></html>
                """;

        assertThat(parser.parseDetail(html, "https://www.pousoalegre.mg.gov.br/concursos_view/2316")).isNull();
    }

    @Test
    @DisplayName("should ignore follow-up notices like convocacao pages")
    void shouldIgnoreFollowUpNoticesLikeConvocacaoPages() {
        String html = """
                <html><body>
                  <table>
                    <tr><td>Tipo:</td><td>Processo Seletivo</td></tr>
                    <tr><td>Súmula:</td><td>EDITAL DE CONVOCAÇÃO - Processo Seletivo 005/2026</td></tr>
                    <tr><td>Nº/ Ano:</td><td>005/2026</td></tr>
                    <tr><td>Data:</td><td>14/01/2026</td></tr>
                    <tr><td>Edital</td><td>Convocação</td><td>14/01/2026</td><td><a href="/arquivo/convocacao.pdf">PDF</a></td></tr>
                  </table>
                </body></html>
                """;

        assertThat(parser.parseDetail(html, "https://www.pousoalegre.mg.gov.br/concursos_view/9999")).isNull();
    }

    @Test
    @DisplayName("should derive edital year from numero ano metadata when title is generic")
    void shouldDeriveEditalYearFromNumeroAnoMetadataWhenTitleIsGeneric() {
        String html = """
                <html><body>
                  <table>
                    <tr><td>Tipo:</td><td>Processo Seletivo</td></tr>
                    <tr><td>Súmula:</td><td>Processo Seletivo</td></tr>
                    <tr><td>Nº/ Ano:</td><td>007/2024</td></tr>
                    <tr><td>Data:</td><td>20/02/2024</td></tr>
                    <tr><td>Edital</td><td>Arquivo principal</td><td>20/02/2024</td><td><a href="/arquivo/ps-007-2024.pdf">PDF</a></td></tr>
                  </table>
                </body></html>
                """;

        PousoAlegreContestPreviewItem item =
                parser.parseDetail(html, "https://www.pousoalegre.mg.gov.br/concursos_view/2400");

        assertThat(item).isNotNull();
        assertThat(item.contestNumber()).isEqualTo("007/2024");
        assertThat(item.editalYear()).isEqualTo(2024);
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = PousoAlegreConcursosParserTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
