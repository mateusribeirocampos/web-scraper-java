package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the PCI parser uses the configured SelectorBundle instead of inline selectors.
 *
 * TDD RED: written before parser wiring is extracted to a bundle.
 */
@Tag("unit")
@DisplayName("PciConcursos selector bundle integration")
class PciConcursosSelectorBundleParserTest {

    @Test
    @DisplayName("should parse representative fixture using selector bundle v1")
    void shouldParseRepresentativeFixtureUsingSelectorBundleV1() throws IOException {
        PciConcursosFixtureParser parser = new PciConcursosFixtureParser(PciConcursosSelectorBundles.v1());

        PciConcursosParsePreview actual = parser.parse(
                fixture("fixtures/pci/pci-concursos-listing.html"),
                "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao"
        );

        assertThat(actual.selectorBundleVersion()).isEqualTo("pci_concursos_v1");
        assertThat(actual.itemsFound()).isEqualTo(2);
        assertThat(actual.items().getFirst().contestName())
                .isEqualTo("Prefeitura de Curitiba abre processo seletivo para Desenvolvedor Java");
    }

    private String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
