package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CamaraSantaRitaProcessosSeletivosParser")
class CamaraSantaRitaProcessosSeletivosParserTest {

    private final CamaraSantaRitaProcessosSeletivosParser parser = new CamaraSantaRitaProcessosSeletivosParser();

    @Test
    @DisplayName("should parse official process blocks and keep only the canonical edital pdf as primary url")
    void shouldParseOfficialProcessBlocksAndKeepOnlyTheCanonicalEditalPdfAsPrimaryUrl() throws Exception {
        String sourceUrl = "https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025";

        assertThat(parser.parse(fixture("fixtures/camara-santa-rita/camara-santa-rita-processos-seletivos-2025.html"), sourceUrl))
                .hasSize(2)
                .first()
                .satisfies(item -> {
                    assertThat(item.contestTitle()).isEqualTo("Edital nº 02/2025: Processo Seletivo Estagiários Nível Superior");
                    assertThat(item.contestNumber()).isEqualTo("02/2025");
                    assertThat(item.editalYear()).isEqualTo(2025);
                    assertThat(item.positionTitle()).isEqualTo("Estagiários Nível Superior");
                    assertThat(item.publishedAt()).isEqualTo(java.time.LocalDate.parse("2025-09-16"));
                    assertThat(item.registrationStartDate()).isEqualTo(java.time.LocalDate.parse("2025-09-19"));
                    assertThat(item.registrationEndDate()).isEqualTo(java.time.LocalDate.parse("2025-09-22"));
                    assertThat(item.editalUrl()).contains("/edital-02-2025/");
                });
    }

    @Test
    @DisplayName("should keep retificacao and homologacao as attachments but not as canonical edital")
    void shouldKeepRetificacaoAndHomologacaoAsAttachmentsButNotAsCanonicalEdital() throws Exception {
        String sourceUrl = "https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025";

        CamaraSantaRitaContestPreviewItem item = parser.parse(
                fixture("fixtures/camara-santa-rita/camara-santa-rita-processos-seletivos-2025.html"),
                sourceUrl
        ).get(1);

        assertThat(item.editalUrl())
                .contains("edital-01-2025-processo-seletivo-simplificado");
        assertThat(item.attachments())
                .extracting(CamaraSantaRitaContestAttachment::label)
                .contains("Retificação nº 01 - Altera e-mail para interposição de recursos e dúvidas, prazo para recurso contra o Edital e Cronograma (clique aqui)",
                        "Divulgação do Termo de Homologação",
                        "Termo de Prorrogação");
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = CamaraSantaRitaProcessosSeletivosParserTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
