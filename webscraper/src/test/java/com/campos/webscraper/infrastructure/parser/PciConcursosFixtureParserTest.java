package com.campos.webscraper.infrastructure.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for the PCI Concursos fixture parser.
 *
 * TDD RED: written before the parser exists.
 */
@Tag("unit")
@DisplayName("PciConcursosFixtureParser")
class PciConcursosFixtureParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("should parse representative PCI fixture and match expected normalized preview output")
    void shouldParseRepresentativePciFixtureAndMatchExpectedNormalizedPreviewOutput() throws IOException {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser();

        String html = fixture("fixtures/pci/pci-concursos-listing.html");
        PciConcursosParsePreview expected = expected("fixtures/pci/pci-concursos-listing-expected.json");

        PciConcursosParsePreview actual = parser.parse(html, "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao");

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.itemsFound()).isEqualTo(2);
        assertThat(actual.selectorBundleVersion()).isEqualTo("pci_concursos_v1");
    }

    @Test
    @DisplayName("should expose unknown education level as null instead of masking it as superior")
    void shouldExposeUnknownEducationLevelAsNullInsteadOfMaskingItAsSuperior() {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser();

        String html = """
                <html><body>
                <article class="ca">
                    <a class="ca-link" href="/concurso/teste">Concurso teste</a>
                    <span class="ca-orgao">Orgao teste</span>
                    <h2 class="ca-cargo">Cargo teste</h2>
                    <li class="ca-escolaridade">Escolaridade a definir</li>
                    <a class="ca-detalhes" href="/concurso/teste/edital">Ver edital</a>
                </article>
                </body></html>
                """;

        PciConcursosParsePreview actual = parser.parse(html, "https://www.pciconcursos.com.br/concursos/teste");

        assertThat(actual.items()).singleElement()
                .extracting(PciConcursosPreviewItem::educationLevel)
                .isNull();
    }

    @Test
    @DisplayName("should keep preview running when detail link is missing")
    void shouldKeepPreviewRunningWhenDetailLinkIsMissing() {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser();

        String html = """
                <html><body>
                <article class="ca">
                    <a class="ca-link" href="/concurso/teste">Concurso teste</a>
                    <span class="ca-orgao">Orgao teste</span>
                    <h2 class="ca-cargo">Cargo teste</h2>
                    <li class="ca-escolaridade">Nivel superior</li>
                </article>
                </body></html>
                """;

        assertThatCode(() -> parser.parse(html, "https://www.pciconcursos.com.br/concursos/teste"))
                .doesNotThrowAnyException();

        PciConcursosParsePreview actual = parser.parse(html, "https://www.pciconcursos.com.br/concursos/teste");

        assertThat(actual.items()).singleElement()
                .extracting(PciConcursosPreviewItem::detailUrl)
                .isNull();
    }

    @Test
    @DisplayName("should resolve relative detail links against source url")
    void shouldResolveRelativeDetailLinksAgainstSourceUrl() {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser();

        String html = """
                <html><body>
                <article class="ca">
                    <a class="ca-link" href="/concursos/teste">Concurso teste</a>
                    <span class="ca-orgao">Orgao teste</span>
                    <h2 class="ca-cargo">Cargo teste</h2>
                    <li class="ca-escolaridade">Nivel superior</li>
                    <a class="ca-detalhes" href="edital">Ver edital</a>
                </article>
                </body></html>
                """;

        PciConcursosParsePreview actual = parser.parse(
                html,
                "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao/pagina-2"
        );

        assertThat(actual.items()).singleElement()
                .extracting(PciConcursosPreviewItem::detailUrl)
                .isEqualTo("https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao/edital");
    }

    @Test
    @DisplayName("should accept accented education labels")
    void shouldAcceptAccentedEducationLabels() {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser();

        String html = """
                <html><body>
                <article class="ca">
                    <a class="ca-link" href="/concurso/teste">Concurso teste</a>
                    <span class="ca-orgao">Orgao teste</span>
                    <h2 class="ca-cargo">Cargo teste</h2>
                    <li class="ca-escolaridade">Nível técnico</li>
                    <a class="ca-detalhes" href="/concurso/teste/edital">Ver edital</a>
                </article>
                </body></html>
                """;

        PciConcursosParsePreview actual = parser.parse(html, "https://www.pciconcursos.com.br/concursos/teste");

        assertThat(actual.items()).singleElement()
                .extracting(PciConcursosPreviewItem::educationLevel)
                .isEqualTo("TECNICO");
    }

    @Test
    @DisplayName("should map pos graduacao style requirements to domain value")
    void shouldMapPosGraduacaoStyleRequirementsToDomainValue() {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser();

        String html = """
                <html><body>
                <article class="ca">
                    <a class="ca-link" href="/concurso/teste">Concurso teste</a>
                    <span class="ca-orgao">Orgao teste</span>
                    <h2 class="ca-cargo">Cargo teste</h2>
                    <li class="ca-escolaridade">Pós-graduação / especialização em TI</li>
                    <a class="ca-detalhes" href="/concurso/teste/edital">Ver edital</a>
                </article>
                </body></html>
                """;

        PciConcursosParsePreview actual = parser.parse(html, "https://www.pciconcursos.com.br/concursos/teste");

        assertThat(actual.items()).singleElement()
                .extracting(PciConcursosPreviewItem::educationLevel)
                .isEqualTo("POS_GRADUACAO");
    }

    @Test
    @DisplayName("should parse vacancy counts with thousands separators")
    void shouldParseVacancyCountsWithThousandsSeparators() {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser();

        String html = """
                <html><body>
                <article class="ca">
                    <a class="ca-link" href="/concurso/teste">Concurso teste</a>
                    <span class="ca-orgao">Orgao teste</span>
                    <h2 class="ca-cargo">Cargo teste</h2>
                    <li class="ca-vagas">1.234 vagas</li>
                    <li class="ca-escolaridade">Nivel medio</li>
                    <a class="ca-detalhes" href="/concurso/teste/edital">Ver edital</a>
                </article>
                </body></html>
                """;

        PciConcursosParsePreview actual = parser.parse(html, "https://www.pciconcursos.com.br/concursos/teste");

        assertThat(actual.items()).singleElement()
                .extracting(PciConcursosPreviewItem::numberOfVacancies)
                .isEqualTo(1234);
    }

    private String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private PciConcursosParsePreview expected(String classpathLocation) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return objectMapper.readValue(inputStream, PciConcursosParsePreview.class);
        }
    }
}
