package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.Map;

/**
 * Factory for versioned PCI Concursos selector bundles.
 */
public final class PciConcursosSelectorBundles {

    private PciConcursosSelectorBundles() {
    }

    public static SelectorBundle v1() {
        return new SelectorBundle(
                "pci_concursos",
                "PciConcursosFixtureParser",
                "1.0.0",
                "pci_concursos_v1",
                LocalDate.of(2026, 3, 12),
                null,
                Map.of(
                        "contestCard", "article.ca",
                        "contestName", ".ca-link",
                        "organizer", ".ca-orgao",
                        "positionTitle", ".ca-cargo",
                        "numberOfVacancies", ".ca-vagas",
                        "educationLevel", ".ca-escolaridade",
                        "salaryRange", ".ca-salario",
                        "registrationDeadline", ".ca-inscricoes",
                        "detailUrl", ".ca-detalhes",
                        "nextPage", "nav.pagination .next"
                )
        );
    }
}
