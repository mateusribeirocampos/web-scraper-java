package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.MunhozConcursosParser;
import com.campos.webscraper.infrastructure.parser.MunhozContestAttachment;
import com.campos.webscraper.infrastructure.parser.MunhozContestPreviewItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("MunhozContestNormalizer")
class MunhozContestNormalizerTest {

    @Test
    @DisplayName("should serialize payload json for parsed Munhoz contest previews")
    void shouldSerializePayloadJsonForParsedMunhozContestPreviews() throws IOException {
        MunhozConcursosParser parser = new MunhozConcursosParser();
        MunhozContestNormalizer normalizer = new MunhozContestNormalizer();

        MunhozContestPreviewItem item = parser.parseDetail(
                fixture("fixtures/munhoz/munhoz-concurso-detail.html"),
                "https://www.munhoz.mg.gov.br/concursos_view/9"
        );

        PublicContestPostingEntity posting = normalizer.normalize(
                item,
                LocalDateTime.parse("2026-03-30T10:20:00")
        );

        assertThat(posting.getExternalId()).isEqualTo("municipal_munhoz:009-2026");
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.parse("2026-03-18"));
        assertThat(posting.getPayloadJson()).contains("\"contestNumber\":\"009/2026\"");
        assertThat(posting.getPayloadJson()).contains("\"editalUrl\":\"https://www.munhoz.mg.gov.br/arquivo/edital-009-2026.pdf\"");
        assertThat(posting.getPayloadJson()).contains("\"publishedAt\":\"2026-03-18\"");
    }

    @Test
    @DisplayName("should sanitize invalid text before serializing payload json")
    void shouldSanitizeInvalidTextBeforeSerializingPayloadJson() {
        MunhozContestNormalizer normalizer = new MunhozContestNormalizer();

        MunhozContestPreviewItem item = new MunhozContestPreviewItem(
                "Processo Seletivo 001/2026 \uD800",
                "Prefeitura \u0000 Munhoz",
                "Cargo \uDFFF",
                null,
                null,
                "001/2026",
                2026,
                "https://www.munhoz.mg.gov.br/concursos_view/1",
                "https://www.munhoz.mg.gov.br/concursos/edital.pdf",
                LocalDate.parse("2026-03-18"),
                null,
                null,
                null,
                java.util.List.of(new MunhozContestAttachment(
                        "Edital",
                        "Descricao \u0001",
                        "https://www.munhoz.mg.gov.br/concursos/edital.pdf"
                )),
                java.util.List.of("Titulo \uD800"),
                java.util.List.of("ANEXO I \u0000")
        );

        PublicContestPostingEntity posting = normalizer.normalize(
                item,
                LocalDateTime.parse("2026-03-30T10:20:00")
        );

        assertThat(posting.getPayloadJson()).doesNotContain("\u0000");
        assertThat(posting.getPayloadJson()).doesNotContain("\u0001");
        assertThat(posting.getPayloadJson()).doesNotContain("\uD800");
        assertThat(posting.getPayloadJson()).doesNotContain("\uDFFF");
        assertThat(posting.getPayloadJson()).contains("Processo Seletivo 001/2026");
    }

    @Test
    @DisplayName("should serialize payload json even with plain object mapper")
    void shouldSerializePayloadJsonEvenWithPlainObjectMapper() {
        MunhozContestNormalizer normalizer = new MunhozContestNormalizer(new ObjectMapper());

        MunhozContestPreviewItem item = new MunhozContestPreviewItem(
                "Processo Seletivo 001/2026",
                "Prefeitura de Munhoz",
                "Analista de Sistemas",
                "SUPERIOR",
                "Graduacao em Ciencia da Computacao",
                "001/2026",
                2026,
                "https://www.munhoz.mg.gov.br/concursos_view/1",
                "https://www.munhoz.mg.gov.br/concursos/edital.pdf",
                LocalDate.parse("2026-03-18"),
                LocalDate.parse("2026-03-20"),
                LocalDate.parse("2026-03-30"),
                LocalDate.parse("2026-04-10"),
                java.util.List.of(new MunhozContestAttachment(
                        "Edital",
                        "Edital 001/2026",
                        "https://www.munhoz.mg.gov.br/concursos/edital.pdf"
                )),
                java.util.List.of("Analista de Sistemas"),
                java.util.List.of("ANEXO I")
        );

        PublicContestPostingEntity posting = normalizer.normalize(
                item,
                LocalDateTime.parse("2026-03-30T10:20:00")
        );

        assertThat(posting.getPayloadJson()).contains("\"publishedAt\":\"2026-03-18\"");
        assertThat(posting.getPayloadJson()).contains("\"registrationEndDate\":\"2026-03-30\"");
        assertThat(posting.getPayloadJson()).contains("\"examDate\":\"2026-04-10\"");
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = MunhozContestNormalizerTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
