package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.CamaraItajubaContestAttachment;
import com.campos.webscraper.infrastructure.parser.CamaraItajubaContestPreviewItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CamaraItajubaContestNormalizer")
class CamaraItajubaContestNormalizerTest {

    @Test
    @DisplayName("should map Câmara Itajubá preview into canonical public contest entity")
    void shouldMapCamaraItajubaPreviewIntoCanonicalPublicContestEntity() {
        CamaraItajubaContestNormalizer normalizer = new CamaraItajubaContestNormalizer();
        CamaraItajubaContestPreviewItem item = new CamaraItajubaContestPreviewItem(
                "Concurso Público 2023",
                "Câmara Municipal de Itajubá",
                "Cargos efetivos diversos",
                "UNKNOWN",
                null,
                2023,
                "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/",
                "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf",
                LocalDate.parse("2023-12-13"),
                LocalDate.parse("2024-02-01"),
                null,
                null,
                12,
                "",
                List.of(new CamaraItajubaContestAttachment(
                        "PDF do Edital",
                        "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf"
                ))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-04-06T10:00:00"));

        assertThat(posting.getExternalId()).isEqualTo("camara_itajuba:concurso-publico-2023");
        assertThat(posting.getCanonicalUrl()).isEqualTo(item.editalUrl());
        assertThat(posting.getOrganizer()).isEqualTo("Câmara Municipal de Itajubá");
        assertThat(posting.getPositionTitle()).isEqualTo("Cargos efetivos diversos");
        assertThat(posting.getGovernmentLevel().name()).isEqualTo("MUNICIPAL");
        assertThat(posting.getState()).isEqualTo("MG");
        assertThat(posting.getEducationLevel().name()).isEqualTo("UNKNOWN");
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.parse("2023-12-13"));
        assertThat(posting.getRegistrationStartDate()).isEqualTo(LocalDate.parse("2024-02-01"));
        assertThat(posting.getRegistrationEndDate()).isNull();
        assertThat(posting.getExamDate()).isNull();
        assertThat(posting.getContestStatus().name()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("should serialize payload json even with plain object mapper")
    void shouldSerializePayloadJsonEvenWithPlainObjectMapper() {
        CamaraItajubaContestNormalizer normalizer = new CamaraItajubaContestNormalizer(new ObjectMapper());
        CamaraItajubaContestPreviewItem item = new CamaraItajubaContestPreviewItem(
                "Concurso Público 2023",
                "Câmara Municipal de Itajubá",
                "Cargos efetivos diversos",
                "UNKNOWN",
                null,
                2023,
                "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/",
                "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf",
                LocalDate.parse("2023-12-13"),
                LocalDate.parse("2024-02-01"),
                null,
                null,
                12,
                "",
                List.of(new CamaraItajubaContestAttachment(
                        "PDF do Edital",
                        "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf"
                ))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-04-06T10:00:00"));

        assertThat(posting.getPayloadJson()).contains("\"publishedAt\":\"2023-12-13\"");
        assertThat(posting.getPayloadJson()).contains("\"registrationStartDate\":\"2024-02-01\"");
        assertThat(posting.getPayloadJson()).contains("\"registrationEndDate\":\"\"");
        assertThat(posting.getPayloadJson()).contains("\"examDate\":\"\"");
        assertThat(posting.getPayloadJson()).contains("\"attachments\":[");
    }

    @Test
    @DisplayName("should not close contest only because edital filename carries a previous year")
    void shouldNotCloseContestOnlyBecauseEditalFilenameCarriesPreviousYear() {
        CamaraItajubaContestNormalizer normalizer = new CamaraItajubaContestNormalizer();
        CamaraItajubaContestPreviewItem item = new CamaraItajubaContestPreviewItem(
                "Concurso Público 2023",
                "Câmara Municipal de Itajubá",
                "Cargos efetivos diversos",
                "UNKNOWN",
                null,
                2023,
                "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/",
                "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf",
                LocalDate.parse("2023-12-13"),
                null,
                null,
                null,
                12,
                "",
                List.of(new CamaraItajubaContestAttachment(
                        "PDF do Edital",
                        "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf"
                ))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2024-01-10T10:00:00"));

        assertThat(posting.getContestStatus().name()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("should keep stable external id even if edital filename changes")
    void shouldKeepStableExternalIdEvenIfEditalFilenameChanges() {
        CamaraItajubaContestNormalizer normalizer = new CamaraItajubaContestNormalizer();
        CamaraItajubaContestPreviewItem item = new CamaraItajubaContestPreviewItem(
                "Concurso Público 2023",
                "Câmara Municipal de Itajubá",
                "Cargos efetivos diversos",
                "UNKNOWN",
                null,
                2023,
                "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/",
                "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2024/01/concurso-publico-retificado.pdf",
                LocalDate.parse("2023-12-13"),
                LocalDate.parse("2024-02-01"),
                LocalDate.parse("2024-02-29"),
                null,
                12,
                "",
                List.of(new CamaraItajubaContestAttachment(
                        "PDF do Edital",
                        "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2024/01/concurso-publico-retificado.pdf"
                ))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-04-06T10:00:00"));

        assertThat(posting.getExternalId()).isEqualTo("camara_itajuba:concurso-publico-2023");
    }

    @Test
    @DisplayName("should keep stable external id across launch and homologation pages of same contest")
    void shouldKeepStableExternalIdAcrossLaunchAndHomologationPagesOfSameContest() {
        CamaraItajubaContestNormalizer normalizer = new CamaraItajubaContestNormalizer();
        CamaraItajubaContestPreviewItem item = new CamaraItajubaContestPreviewItem(
                "Homologação do Concurso Público 2023",
                "Câmara Municipal de Itajubá",
                "Cargos efetivos diversos",
                "UNKNOWN",
                null,
                2023,
                "https://itajuba.cam.mg.gov.br/site/homologacao-do-concurso-publico-para-preenchimento-de-cargos-efetivos/",
                "https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2024/01/concurso-publico-retificado.pdf",
                LocalDate.parse("2024-06-01"),
                LocalDate.parse("2024-02-01"),
                null,
                null,
                12,
                "",
                List.of()
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-04-06T10:00:00"));

        assertThat(posting.getExternalId()).isEqualTo("camara_itajuba:concurso-publico-2023");
    }
}
