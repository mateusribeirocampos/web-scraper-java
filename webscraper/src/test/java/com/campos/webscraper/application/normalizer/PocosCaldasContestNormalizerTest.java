package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.infrastructure.parser.PocosCaldasContestPreviewItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PocosCaldasContestNormalizer")
class PocosCaldasContestNormalizerTest {

    @Test
    @DisplayName("should map Poços de Caldas preview item into canonical public contest entity")
    void shouldMapPocosDeCaldasPreviewItemIntoCanonicalPublicContestEntity() {
        PocosCaldasContestNormalizer normalizer = new PocosCaldasContestNormalizer(
                new ObjectMapper().findAndRegisterModules()
        );

        var posting = normalizer.normalize(
                new PocosCaldasContestPreviewItem(
                        "Edital de Processo Seletivo Simplificado nº 001/2025",
                        "Prefeitura Municipal de Poços de Caldas",
                        "Processo seletivo simplificado para múltiplos cargos",
                        "UNKNOWN",
                        "001/2025",
                        2025,
                        "https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf",
                        null,
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 9, 9),
                        null,
                        null,
                        "Vencimentos variáveis conforme Anexo IV do edital",
                        "EDITAL DE PROCESSO SELETIVO SIMPLIFICADO Nº 001/2025"
                ),
                LocalDateTime.of(2025, 9, 2, 10, 0)
        );

        assertThat(posting.getExternalId()).isEqualTo("municipal_pocos_caldas:processo-seletivo-001-2025");
        assertThat(posting.getCanonicalUrl()).isEqualTo("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf");
        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.OPEN);
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(posting.getPayloadJson()).contains("001/2025");
    }

    @Test
    @DisplayName("should mark contest as closed after registration end date")
    void shouldMarkContestAsClosedAfterRegistrationEndDate() {
        PocosCaldasContestNormalizer normalizer = new PocosCaldasContestNormalizer(
                new ObjectMapper().findAndRegisterModules()
        );

        var posting = normalizer.normalize(
                new PocosCaldasContestPreviewItem(
                        "Edital de Processo Seletivo Simplificado nº 001/2025",
                        "Prefeitura Municipal de Poços de Caldas",
                        "Processo seletivo simplificado para múltiplos cargos",
                        "UNKNOWN",
                        "001/2025",
                        2025,
                        "https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf",
                        null,
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 9, 9),
                        null,
                        null,
                        "Vencimentos variáveis conforme Anexo IV do edital",
                        "EDITAL DE PROCESSO SELETIVO SIMPLIFICADO Nº 001/2025"
                ),
                LocalDateTime.of(2025, 9, 10, 10, 0)
        );

        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.REGISTRATION_CLOSED);
    }

    @Test
    @DisplayName("should preserve explicit published date when source provides one")
    void shouldPreserveExplicitPublishedDateWhenSourceProvidesOne() {
        PocosCaldasContestNormalizer normalizer = new PocosCaldasContestNormalizer(
                new ObjectMapper().findAndRegisterModules()
        );

        var posting = normalizer.normalize(
                new PocosCaldasContestPreviewItem(
                        "Edital de Processo Seletivo Simplificado nº 001/2025",
                        "Prefeitura Municipal de Poços de Caldas",
                        "Processo seletivo simplificado para múltiplos cargos",
                        "UNKNOWN",
                        "001/2025",
                        2025,
                        "https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf",
                        LocalDate.of(2025, 8, 28),
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 9, 9),
                        null,
                        null,
                        "Vencimentos variáveis conforme Anexo IV do edital",
                        "EDITAL DE PROCESSO SELETIVO SIMPLIFICADO Nº 001/2025"
                ),
                LocalDateTime.of(2025, 9, 2, 10, 0)
        );

        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2025, 8, 28));
    }

    @Test
    @DisplayName("should mark undated edital as closed once edital year has passed")
    void shouldMarkUndatedEditalAsClosedOnceEditalYearHasPassed() {
        PocosCaldasContestNormalizer normalizer = new PocosCaldasContestNormalizer(
                new ObjectMapper().findAndRegisterModules()
        );

        var posting = normalizer.normalize(
                new PocosCaldasContestPreviewItem(
                        "Edital de Processo Seletivo Simplificado nº 001/2025",
                        "Prefeitura Municipal de Poços de Caldas",
                        "Processo seletivo simplificado para múltiplos cargos",
                        "UNKNOWN",
                        "001/2025",
                        2025,
                        "https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Vencimentos variáveis conforme Anexo IV do edital",
                        "EDITAL DE PROCESSO SELETIVO SIMPLIFICADO Nº 001/2025"
                ),
                LocalDateTime.of(2026, 4, 9, 10, 0)
        );

        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.REGISTRATION_CLOSED);
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2025, 1, 1));
    }
}
