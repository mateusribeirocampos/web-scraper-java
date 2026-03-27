package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.InconfidentesContestAttachment;
import com.campos.webscraper.infrastructure.parser.InconfidentesContestPreviewItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("InconfidentesContestNormalizer")
class InconfidentesContestNormalizerTest {

    @Test
    @DisplayName("should map Inconfidentes municipal preview into canonical public contest entity")
    void shouldMapInconfidentesMunicipalPreviewIntoCanonicalPublicContestEntity() {
        InconfidentesContestNormalizer normalizer = new InconfidentesContestNormalizer();

        InconfidentesContestPreviewItem item = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL 001/2026 - PROCESSO SELETIVO 001/2026 - CONTRATACAO DE PROFESSOR",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "Professor",
                "SUPERIOR",
                2026,
                "https://ecrie.com.br/edital-001-2026.pdf",
                "https://ecrie.com.br/edital-001-2026.pdf",
                List.of(
                        new InconfidentesContestAttachment("Edital", "https://ecrie.com.br/edital-001-2026.pdf"),
                        new InconfidentesContestAttachment("Resultado Final", "https://ecrie.com.br/resultado-final.pdf")
                )
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-25T10:15:00"));

        assertThat(posting.getExternalId())
                .isEqualTo("municipal_inconfidentes:departamento-de-educacao-001-2026");
        assertThat(posting.getCanonicalUrl())
                .isEqualTo("https://ecrie.com.br/edital-001-2026.pdf");
        assertThat(posting.getOrganizer()).isEqualTo("Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO");
        assertThat(posting.getPositionTitle()).isEqualTo("Professor");
        assertThat(posting.getGovernmentLevel().name()).isEqualTo("MUNICIPAL");
        assertThat(posting.getState()).isEqualTo("MG");
        assertThat(posting.getEducationLevel().name()).isEqualTo("SUPERIOR");
        assertThat(posting.getEditalUrl()).isEqualTo("https://ecrie.com.br/edital-001-2026.pdf");
        assertThat(posting.getPublishedAt()).isEqualTo(java.time.LocalDate.parse("2026-01-01"));
        assertThat(posting.getContestStatus().name()).isEqualTo("OPEN");
        assertThat(posting.getPayloadJson()).contains("Resultado Final");
    }

    @Test
    @DisplayName("should reject preview items without a stable edital url")
    void shouldRejectPreviewItemsWithoutAStableEditalUrl() {
        InconfidentesContestNormalizer normalizer = new InconfidentesContestNormalizer();

        InconfidentesContestPreviewItem item = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL 005/2026 - PROCESSO SELETIVO 005/2026 - CONTRATACAO DE PROFESSOR",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "Professor",
                "SUPERIOR",
                2026,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> normalizer.normalize(item, LocalDateTime.parse("2026-03-25T10:15:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stable edital URL");
    }

    @Test
    @DisplayName("should keep stable identifier even when edital pdf url changes")
    void shouldKeepStableIdentifierEvenWhenEditalPdfUrlChanges() {
        InconfidentesContestNormalizer normalizer = new InconfidentesContestNormalizer();

        InconfidentesContestPreviewItem original = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL 008/2026 - PROCESSO SELETIVO 008/2026 - CONTRATACAO DE PROFESSOR",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "Professor",
                "SUPERIOR",
                2026,
                "https://ecrie.com.br/edital-original.pdf",
                "https://ecrie.com.br/edital-original.pdf",
                List.of(new InconfidentesContestAttachment("Edital 008/2026", "https://ecrie.com.br/edital-original.pdf"))
        );
        InconfidentesContestPreviewItem republished = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL 008/2026 - PROCESSO SELETIVO 008/2026 - CONTRATACAO DE PROFESSOR",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "Professor",
                "SUPERIOR",
                2026,
                "https://ecrie.com.br/edital-retificado.pdf",
                "https://ecrie.com.br/edital-retificado.pdf",
                List.of(new InconfidentesContestAttachment("Edital retificado 008/2026", "https://ecrie.com.br/edital-retificado.pdf"))
        );

        PublicContestPostingEntity originalPosting = normalizer.normalize(original, LocalDateTime.parse("2026-03-25T10:15:00"));
        PublicContestPostingEntity republishedPosting = normalizer.normalize(republished, LocalDateTime.parse("2026-03-26T10:15:00"));

        assertThat(republishedPosting.getExternalId()).isEqualTo(originalPosting.getExternalId());
        assertThat(republishedPosting.getCanonicalUrl()).isEqualTo("https://ecrie.com.br/edital-retificado.pdf");
        assertThat(republishedPosting.getEditalUrl()).isEqualTo("https://ecrie.com.br/edital-retificado.pdf");
    }

    @Test
    @DisplayName("should keep stable identifier when listing title wording changes but edital number stays the same")
    void shouldKeepStableIdentifierWhenListingTitleWordingChangesButEditalNumberStaysTheSame() {
        InconfidentesContestNormalizer normalizer = new InconfidentesContestNormalizer();

        InconfidentesContestPreviewItem original = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL 011/2026 - PROCESSO SELETIVO 011/2026 - CONTRATACAO DE PROFESSOR",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "Professor",
                "SUPERIOR",
                2026,
                "https://ecrie.com.br/edital-011.pdf",
                "https://ecrie.com.br/edital-011.pdf",
                List.of(new InconfidentesContestAttachment("Edital 011/2026", "https://ecrie.com.br/edital-011.pdf"))
        );
        InconfidentesContestPreviewItem retitled = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL 011/2026 - PROCESSO SELETIVO 011/2026 - CONTRATACAO DE PROFESSOR RETIFICADO",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "Professor",
                "SUPERIOR",
                2026,
                "https://ecrie.com.br/edital-011-retificado.pdf",
                "https://ecrie.com.br/edital-011-retificado.pdf",
                List.of(new InconfidentesContestAttachment("Edital retificado 011/2026", "https://ecrie.com.br/edital-011-retificado.pdf"))
        );

        PublicContestPostingEntity originalPosting = normalizer.normalize(original, LocalDateTime.parse("2026-03-25T10:15:00"));
        PublicContestPostingEntity retitledPosting = normalizer.normalize(retitled, LocalDateTime.parse("2026-03-26T10:15:00"));

        assertThat(retitledPosting.getExternalId()).isEqualTo(originalPosting.getExternalId());
        assertThat(retitledPosting.getCanonicalUrl()).isEqualTo("https://ecrie.com.br/edital-011-retificado.pdf");
    }

    @Test
    @DisplayName("should fall back to crawl day only when edital year is unavailable")
    void shouldFallBackToCrawlDayOnlyWhenEditalYearIsUnavailable() {
        InconfidentesContestNormalizer normalizer = new InconfidentesContestNormalizer();

        InconfidentesContestPreviewItem item = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL EXTRAORDINARIO 2026",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "EDITAL EXTRAORDINARIO 2026",
                null,
                null,
                "https://ecrie.com.br/edital-extra.pdf",
                "https://ecrie.com.br/edital-extra.pdf",
                List.of(new InconfidentesContestAttachment("Edital extraordinario", "https://ecrie.com.br/edital-extra.pdf"))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-25T10:15:00"));

        assertThat(posting.getPublishedAt()).isEqualTo(java.time.LocalDate.parse("2026-03-25"));
        assertThat(posting.getContestStatus().name()).isEqualTo("OPEN");
        assertThat(posting.getEducationLevel().name()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("should mark obviously historical editais as registration closed")
    void shouldMarkObviouslyHistoricalEditaisAsRegistrationClosed() {
        InconfidentesContestNormalizer normalizer = new InconfidentesContestNormalizer();

        InconfidentesContestPreviewItem item = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE EDUCACAO",
                "EDITAL 003/2024 - PROCESSO SELETIVO 003/2024 - CONTRATACAO DE PROFESSOR",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO",
                "Professor",
                "SUPERIOR",
                2024,
                "https://ecrie.com.br/edital-2024.pdf",
                "https://ecrie.com.br/edital-2024.pdf",
                List.of(new InconfidentesContestAttachment("Edital 003/2024", "https://ecrie.com.br/edital-2024.pdf"))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-25T10:15:00"));

        assertThat(posting.getContestStatus().name()).isEqualTo("REGISTRATION_CLOSED");
    }

    @Test
    @DisplayName("should preserve unknown education when title does not expose schooling")
    void shouldPreserveUnknownEducationWhenTitleDoesNotExposeSchooling() {
        InconfidentesContestNormalizer normalizer = new InconfidentesContestNormalizer();

        InconfidentesContestPreviewItem item = new InconfidentesContestPreviewItem(
                "DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 015/2026",
                "Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE ADMINISTRACAO",
                "EDITAL 015/2026",
                null,
                2026,
                "https://ecrie.com.br/edital-015.pdf",
                "https://ecrie.com.br/edital-015.pdf",
                List.of(new InconfidentesContestAttachment("Edital 015/2026", "https://ecrie.com.br/edital-015.pdf"))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-25T10:15:00"));

        assertThat(posting.getEducationLevel().name()).isEqualTo("UNKNOWN");
    }
}
