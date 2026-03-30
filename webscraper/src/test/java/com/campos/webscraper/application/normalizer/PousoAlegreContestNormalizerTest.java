package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.PousoAlegreContestAttachment;
import com.campos.webscraper.infrastructure.parser.PousoAlegreContestPreviewItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PousoAlegreContestNormalizer")
class PousoAlegreContestNormalizerTest {

    @Test
    @DisplayName("should serialize payload json even with plain object mapper")
    void shouldSerializePayloadJsonEvenWithPlainObjectMapper() {
        PousoAlegreContestNormalizer normalizer = new PousoAlegreContestNormalizer(new ObjectMapper());

        PousoAlegreContestPreviewItem item = new PousoAlegreContestPreviewItem(
                "Processo Seletivo Simplificado nº 005/2026",
                "Prefeitura Municipal de Pouso Alegre",
                "Analista de Sistemas",
                "SUPERIOR",
                "Graduacao em Ciencia da Computacao",
                "005/2026",
                2026,
                "https://www.pousoalegre.mg.gov.br/concursos_view/2314",
                "https://www.pousoalegre.mg.gov.br/arquivo/edital-005-2026.pdf",
                LocalDate.parse("2026-01-14"),
                LocalDate.parse("2026-01-15"),
                LocalDate.parse("2026-01-30"),
                LocalDate.parse("2026-02-10"),
                java.util.List.of(new PousoAlegreContestAttachment(
                        "Edital",
                        "Edital 005/2026",
                        "https://www.pousoalegre.mg.gov.br/arquivo/edital-005-2026.pdf"
                )),
                java.util.List.of("Analista de Sistemas"),
                java.util.List.of("ANEXO I")
        );

        PublicContestPostingEntity posting = normalizer.normalize(
                item,
                LocalDateTime.parse("2026-03-30T10:20:00")
        );

        assertThat(posting.getPayloadJson()).contains("\"publishedAt\":\"2026-01-14\"");
        assertThat(posting.getPayloadJson()).contains("\"registrationEndDate\":\"2026-01-30\"");
        assertThat(posting.getPayloadJson()).contains("\"examDate\":\"2026-02-10\"");
    }
}
