package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.infrastructure.parser.ExtremaContestAttachment;
import com.campos.webscraper.infrastructure.parser.ExtremaContestPreviewItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("ExtremaContestNormalizer")
class ExtremaContestNormalizerTest {

    private final ExtremaContestNormalizer normalizer = new ExtremaContestNormalizer();

    @Test
    @DisplayName("should build stable municipal Extrema contest posting from preview item")
    void shouldBuildStableMunicipalExtremaContestPostingFromPreviewItem() {
        ExtremaContestPreviewItem item = new ExtremaContestPreviewItem(
                "Processo seletivo público simplificado para contratação de cargos/funções públicas para o quadro da Educação de Extrema",
                "Prefeitura Municipal de Extrema",
                "Professor de Educação Básica",
                "SUPERIOR",
                "Licenciatura plena",
                "003/2025",
                2025,
                "https://www.extrema.mg.gov.br/secretarias/educacao/processo-seletivo-publico",
                "https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_17072025170837.pdf",
                LocalDate.of(2025, 7, 17),
                LocalDate.of(2025, 7, 18),
                LocalDate.of(2025, 7, 31),
                LocalDate.of(2025, 8, 10),
                List.of(new ExtremaContestAttachment("Edital nº003/2025", "https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_17072025170837.pdf")),
                List.of("Professor de Educação Básica"),
                List.of("Anexo I")
        );

        var posting = normalizer.normalize(item, LocalDateTime.of(2026, 4, 10, 12, 0));

        assertThat(posting.getExternalId()).isEqualTo("municipal_extrema:003-2025");
        assertThat(posting.getEducationLevel()).isEqualTo(EducationLevel.SUPERIOR);
        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.REGISTRATION_CLOSED);
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2025, 7, 17));
    }

    @Test
    @DisplayName("should close stale editais without registration dates once year has passed")
    void shouldCloseStaleEditaisWithoutRegistrationDatesOnceYearHasPassed() {
        ExtremaContestPreviewItem item = new ExtremaContestPreviewItem(
                "Edital de Seleção de Formadores do Leei",
                "Prefeitura Municipal de Extrema",
                "Formadores do Leei",
                null,
                null,
                "001/2025",
                2025,
                "https://www.extrema.mg.gov.br/secretarias/educacao/edital-de-selecao-de-formadores-do-leei",
                "https://ecrie.com.br/formadores-001-2025.pdf",
                null,
                null,
                null,
                null,
                List.of(new ExtremaContestAttachment("Edital 001/2025", "https://ecrie.com.br/formadores-001-2025.pdf")),
                List.of(),
                List.of()
        );

        var posting = normalizer.normalize(item, LocalDateTime.of(2026, 4, 10, 12, 0));

        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.REGISTRATION_CLOSED);
    }

    @Test
    @DisplayName("should mark same-year contest as exam scheduled when exam date exists but registration end date is missing")
    void shouldMarkSameYearContestAsExamScheduledWhenExamDateExistsButRegistrationEndDateIsMissing() {
        ExtremaContestPreviewItem item = new ExtremaContestPreviewItem(
                "Processo seletivo simplificado para contratação temporária",
                "Prefeitura Municipal de Extrema",
                "Professor de Apoio",
                "SUPERIOR",
                null,
                "004/2026",
                2026,
                "https://www.extrema.mg.gov.br/secretarias/educacao/processo-seletivo-004-2026",
                "https://ecrie.com.br/edital-004-2026.pdf",
                null,
                null,
                null,
                LocalDate.of(2026, 5, 20),
                List.of(new ExtremaContestAttachment("Edital 004/2026", "https://ecrie.com.br/edital-004-2026.pdf")),
                List.of(),
                List.of()
        );

        var posting = normalizer.normalize(item, LocalDateTime.of(2026, 5, 10, 12, 0));

        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.EXAM_SCHEDULED);
    }

    @Test
    @DisplayName("should close same-year contest when only exam date exists and exam has already passed")
    void shouldCloseSameYearContestWhenOnlyExamDateExistsAndExamHasAlreadyPassed() {
        ExtremaContestPreviewItem item = new ExtremaContestPreviewItem(
                "Processo seletivo simplificado para contratação temporária",
                "Prefeitura Municipal de Extrema",
                "Professor de Apoio",
                "SUPERIOR",
                null,
                "004/2026",
                2026,
                "https://www.extrema.mg.gov.br/secretarias/educacao/processo-seletivo-004-2026",
                "https://ecrie.com.br/edital-004-2026.pdf",
                null,
                null,
                null,
                LocalDate.of(2026, 5, 20),
                List.of(new ExtremaContestAttachment("Edital 004/2026", "https://ecrie.com.br/edital-004-2026.pdf")),
                List.of(),
                List.of()
        );

        var posting = normalizer.normalize(item, LocalDateTime.of(2026, 6, 10, 12, 0));

        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.REGISTRATION_CLOSED);
    }
}
