package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.CamaraSantaRitaContestAttachment;
import com.campos.webscraper.infrastructure.parser.CamaraSantaRitaContestPreviewItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CamaraSantaRitaContestNormalizer")
class CamaraSantaRitaContestNormalizerTest {

    @Test
    @DisplayName("should map Câmara Santa Rita preview into canonical public contest entity")
    void shouldMapCamaraSantaRitaPreviewIntoCanonicalPublicContestEntity() {
        CamaraSantaRitaContestNormalizer normalizer = new CamaraSantaRitaContestNormalizer();
        CamaraSantaRitaContestPreviewItem item = new CamaraSantaRitaContestPreviewItem(
                "Edital nº 01/2025: Processo Seletivo Estagiários Nível Superior",
                "Câmara Municipal de Santa Rita do Sapucaí",
                "Estagiários Nível Superior",
                "SUPERIOR",
                "01/2025",
                2025,
                "https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025",
                "https://www.santaritadosapucai.mg.leg.br/atividades/arquivos-pdfs/editais/edital-01-2025-processo-seletivo-simplificado-para-recrutamento-de-interessados-para-o-programa-de-estagios-da-camara-municipal/at_download/file",
                LocalDate.parse("2025-01-06"),
                LocalDate.parse("2025-01-13"),
                LocalDate.parse("2025-01-19"),
                List.of(new CamaraSantaRitaContestAttachment(
                        "Divulgação do Edital (clique aqui)",
                        "https://www.santaritadosapucai.mg.leg.br/atividades/arquivos-pdfs/editais/edital-01-2025-processo-seletivo-simplificado-para-recrutamento-de-interessados-para-o-programa-de-estagios-da-camara-municipal/at_download/file"
                ))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-04-05T10:00:00"));

        assertThat(posting.getExternalId()).isEqualTo("camara_santa_rita_sapucai:01-2025");
        assertThat(posting.getCanonicalUrl()).isEqualTo(item.editalUrl());
        assertThat(posting.getOrganizer()).isEqualTo("Câmara Municipal de Santa Rita do Sapucaí");
        assertThat(posting.getPositionTitle()).isEqualTo("Estagiários Nível Superior");
        assertThat(posting.getGovernmentLevel().name()).isEqualTo("MUNICIPAL");
        assertThat(posting.getState()).isEqualTo("MG");
        assertThat(posting.getEducationLevel().name()).isEqualTo("SUPERIOR");
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.parse("2025-01-06"));
        assertThat(posting.getRegistrationEndDate()).isEqualTo(LocalDate.parse("2025-01-19"));
        assertThat(posting.getContestStatus().name()).isEqualTo("REGISTRATION_CLOSED");
    }

    @Test
    @DisplayName("should serialize payload json even with plain object mapper")
    void shouldSerializePayloadJsonEvenWithPlainObjectMapper() {
        CamaraSantaRitaContestNormalizer normalizer = new CamaraSantaRitaContestNormalizer(new ObjectMapper());
        CamaraSantaRitaContestPreviewItem item = new CamaraSantaRitaContestPreviewItem(
                "Edital nº 01/2025: Processo Seletivo Estagiários Nível Superior",
                "Câmara Municipal de Santa Rita do Sapucaí",
                "Estagiários Nível Superior",
                "SUPERIOR",
                "01/2025",
                2025,
                "https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025",
                "https://www.santaritadosapucai.mg.leg.br/atividades/arquivos-pdfs/editais/edital-01-2025-processo-seletivo-simplificado-para-recrutamento-de-interessados-para-o-programa-de-estagios-da-camara-municipal/at_download/file",
                LocalDate.parse("2025-01-06"),
                LocalDate.parse("2025-01-13"),
                LocalDate.parse("2025-01-19"),
                List.of(new CamaraSantaRitaContestAttachment(
                        "Divulgação do Edital (clique aqui)",
                        "https://www.santaritadosapucai.mg.leg.br/atividades/arquivos-pdfs/editais/edital-01-2025-processo-seletivo-simplificado-para-recrutamento-de-interessados-para-o-programa-de-estagios-da-camara-municipal/at_download/file"
                ))
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-04-05T10:00:00"));

        assertThat(posting.getPayloadJson()).contains("\"publishedAt\":\"2025-01-06\"");
        assertThat(posting.getPayloadJson()).contains("\"registrationEndDate\":\"2025-01-19\"");
        assertThat(posting.getPayloadJson()).contains("\"attachments\":[");
    }
}
