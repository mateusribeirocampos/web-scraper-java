package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for selector bundles used by static HTML parsers.
 *
 * TDD RED: written before SelectorBundle exists.
 */
@Tag("unit")
@DisplayName("SelectorBundle")
class SelectorBundleTest {

    @Test
    @DisplayName("should expose pci concursos v1 selectors with stable metadata")
    void shouldExposePciConcursosV1SelectorsWithStableMetadata() {
        SelectorBundle bundle = PciConcursosSelectorBundles.v1();

        assertThat(bundle.siteCode()).isEqualTo("pci_concursos");
        assertThat(bundle.strategyName()).isEqualTo("PciConcursosFixtureParser");
        assertThat(bundle.parserVersion()).isEqualTo("1.0.0");
        assertThat(bundle.selectorBundleVersion()).isEqualTo("pci_concursos_v1");
        assertThat(bundle.effectiveFrom()).isEqualTo(LocalDate.of(2026, 3, 12));
        assertThat(bundle.deprecatedAt()).isNull();
        assertThat(bundle.selectors()).containsEntry("contestCard", "article.ca");
        assertThat(bundle.selectors()).containsEntry("contestName", ".ca-link");
        assertThat(bundle.selectors()).containsEntry("organizer", ".ca-orgao");
        assertThat(bundle.selectors()).containsEntry("numberOfVacancies", ".ca-vagas");
        assertThat(bundle.selectors()).containsEntry("salaryRange", ".ca-salario");
        assertThat(bundle.selectors()).containsEntry("registrationDeadline", ".ca-inscricoes");
        assertThat(bundle.selectors()).containsEntry("detailUrl", ".ca-detalhes");
    }

    @Test
    @DisplayName("should reject incomplete selector bundle before parser execution")
    void shouldRejectIncompleteSelectorBundleBeforeParserExecution() {
        SelectorBundle incompleteBundle = new SelectorBundle(
                "pci_concursos",
                "PciConcursosFixtureParser",
                "1.0.0",
                "pci_concursos_v1_broken",
                LocalDate.of(2026, 3, 12),
                null,
                Map.of(
                        "contestCard", "article.ca",
                        "contestName", ".ca-link"
                )
        );

        assertThatThrownBy(() -> new PciConcursosFixtureParser(incompleteBundle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pci_concursos_v1_broken")
                .hasMessageContaining("organizer");
    }
}
