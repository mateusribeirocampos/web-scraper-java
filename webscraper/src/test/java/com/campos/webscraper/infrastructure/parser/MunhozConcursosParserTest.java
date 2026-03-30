package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("MunhozConcursosParser")
class MunhozConcursosParserTest {

    private final MunhozConcursosParser parser = new MunhozConcursosParser();

    @Test
    @DisplayName("should extract detail urls from Munhoz concursos listing page")
    void shouldExtractDetailUrlsFromMunhozConcursosListingPage() throws IOException {
        String sourceUrl = "https://www.munhoz.mg.gov.br/concursos-publicos";

        assertThat(parser.parseListingUrls(fixture("fixtures/munhoz/munhoz-concursos-listing.html"), sourceUrl))
                .containsExactly(
                        "https://www.munhoz.mg.gov.br/concursos_view/9",
                        "https://www.munhoz.mg.gov.br/concursos_view/11"
                );
    }

    @Test
    @DisplayName("should parse structured contest detail and select canonical edital attachment by description")
    void shouldParseStructuredContestDetailAndSelectCanonicalEditalAttachmentByDescription() throws IOException {
        String sourceUrl = "https://www.munhoz.mg.gov.br/concursos_view/9";

        MunhozContestPreviewItem item = parser.parseDetail(
                fixture("fixtures/munhoz/munhoz-concurso-detail.html"),
                sourceUrl
        );

        assertThat(item).isNotNull();
        assertThat(item.contestTitle()).contains("PROCESSO SELETIVO SIMPLIFICADO Nº 009/2026");
        assertThat(item.organizer()).isEqualTo("Prefeitura Municipal de Munhoz");
        assertThat(item.contestNumber()).isEqualTo("009/2026");
        assertThat(item.editalYear()).isEqualTo(2026);
        assertThat(item.editalUrl()).isEqualTo("https://www.munhoz.mg.gov.br/arquivo/edital-009-2026.pdf");
        assertThat(item.contestUrl()).isEqualTo(sourceUrl);
    }

    @Test
    @DisplayName("should ignore non contest municipal pages like chamamento publico")
    void shouldIgnoreNonContestMunicipalPagesLikeChamamentoPublico() {
        String html = """
                <html><body>
                  <table>
                    <tr><td>Tipo:</td><td>Chamamento Público</td></tr>
                    <tr><td>Súmula:</td><td>Edital de Chamamento Público 01/2026</td></tr>
                    <tr><td>Data:</td><td>01/03/2026</td></tr>
                    <tr><td>Edital</td><td>Chamamento 01/2026</td><td>01/03/2026</td><td><a href="/arquivo/chamamento.pdf">PDF</a></td></tr>
                  </table>
                </body></html>
                """;

        assertThat(parser.parseDetail(html, "https://www.munhoz.mg.gov.br/concursos_view/11")).isNull();
    }

    @Test
    @DisplayName("should treat concurso publico detail pages as valid public contests")
    void shouldTreatConcursoPublicoDetailPagesAsValidPublicContests() {
        String html = """
                <html><body>
                  <table>
                    <tr><td>Tipo:</td><td>Concurso Público</td></tr>
                    <tr><td>Súmula:</td><td>Concurso Público 003/2026</td></tr>
                    <tr><td>Nº/ Ano:</td><td>003/2026</td></tr>
                    <tr><td>Data:</td><td>10/03/2026</td></tr>
                    <tr><td>Edital</td><td>Concurso Público 003/2026</td><td>10/03/2026</td><td><a href="/arquivo/concurso-003-2026.pdf">PDF</a></td></tr>
                  </table>
                </body></html>
                """;

        MunhozContestPreviewItem item =
                parser.parseDetail(html, "https://www.munhoz.mg.gov.br/concursos_view/33");

        assertThat(item).isNotNull();
        assertThat(item.contestNumber()).isEqualTo("003/2026");
        assertThat(item.editalUrl()).endsWith("/arquivo/concurso-003-2026.pdf");
    }

    @Test
    @DisplayName("should ignore retificacao attachment when selecting canonical edital")
    void shouldIgnoreRetificacaoAttachmentWhenSelectingCanonicalEdital() {
        String html = """
                <html><body>
                  <table>
                    <tr><td>Tipo:</td><td>Processos Seletivos</td></tr>
                    <tr><td>Súmula:</td><td>Processo Seletivo 009/2026</td></tr>
                    <tr><td>Nº/ Ano:</td><td>009/2026</td></tr>
                    <tr><td>Data:</td><td>18/03/2026</td></tr>
                    <tr><td>Edital</td><td>Retificação do Edital 009/2026</td><td>19/03/2026</td><td><a href="/arquivo/retificacao-009-2026.pdf">PDF</a></td></tr>
                    <tr><td>Edital</td><td>Edital de Processo Seletivo 009/2026</td><td>18/03/2026</td><td><a href="/arquivo/edital-009-2026.pdf">PDF</a></td></tr>
                  </table>
                </body></html>
                """;

        MunhozContestPreviewItem item =
                parser.parseDetail(html, "https://www.munhoz.mg.gov.br/concursos_view/9");

        assertThat(item).isNotNull();
        assertThat(item.editalUrl()).endsWith("/arquivo/edital-009-2026.pdf");
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = MunhozConcursosParserTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
