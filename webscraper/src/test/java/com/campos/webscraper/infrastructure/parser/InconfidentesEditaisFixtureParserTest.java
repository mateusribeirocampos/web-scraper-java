package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("InconfidentesEditaisFixtureParser")
class InconfidentesEditaisFixtureParserTest {

    @Test
    @DisplayName("should parse representative Inconfidentes fixture and keep only operational contest blocks")
    void shouldParseRepresentativeInconfidentesFixtureAndKeepOnlyOperationalContestBlocks() throws IOException {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        InconfidentesParsePreview actual = parser.parse(
                fixture("fixtures/inconfidentes/inconfidentes-editais-listing.html"),
                "https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos"
        );

        assertThat(actual.itemsFound()).isEqualTo(1);
        assertThat(actual.items()).singleElement().satisfies(item -> {
            assertThat(item.department()).isEqualTo("DEPARTAMENTO DE EDUCACAO");
            assertThat(item.contestTitle())
                    .isEqualTo("EDITAL 001/2026 - PROCESSO SELETIVO 001/2026 - CONTRATACAO DE PROFESSOR");
            assertThat(item.organizer())
                    .isEqualTo("Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO");
            assertThat(item.positionTitle()).isEqualTo("Professor");
            assertThat(item.educationLevel()).isEqualTo("SUPERIOR");
            assertThat(item.editalYear()).isEqualTo(2026);
            assertThat(item.editalUrl()).isEqualTo("https://ecrie.com.br/edital-001-2026.pdf");
            assertThat(item.attachments()).hasSize(4);
        });
    }

    @Test
    @DisplayName("should ignore non contest notices such as transport and cultural calls")
    void shouldIgnoreNonContestNoticesSuchAsTransportAndCulturalCalls() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">EDITAL 002/2026 - FORMACAO DE RESERVA PARA O PROGRAMA DE TRANSPORTE COLETIVO UNIVERSITARIO</p>
                    <p><a href="https://example.com/transporte.pdf">Edital transporte</a></p>
                    <p class="department">DEPARTAMENTO DE CULTURA</p>
                    <p class="contest-title">CHAMAMENTO PUBLICO - PREMIACAO DE BLOCOS - CARNAVAL 2026</p>
                    <p><a href="https://example.com/cultura.pdf">Edital cultura</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.itemsFound()).isZero();
        assertThat(actual.items()).isEmpty();
    }

    @Test
    @DisplayName("should choose edital link by label instead of attachment order")
    void shouldChooseEditalLinkByLabelInsteadOfAttachmentOrder() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">EDITAL 003/2026 - PROCESSO SELETIVO 003/2026 - CONTRATACAO DE ANALISTA</p>
                    <p><a href="https://example.com/resultado.pdf">Resultado final do processo seletivo 003/2026</a></p>
                    <p><a href="https://example.com/gabarito.pdf">Gabarito do concurso 003/2026</a></p>
                    <p><a href="https://example.com/edital.pdf">EDITAL 003/2026 - PROCESSO SELETIVO 003/2026</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.items()).singleElement().satisfies(item -> {
            assertThat(item.editalUrl()).isEqualTo("https://example.com/edital.pdf");
            assertThat(item.contestUrl()).isEqualTo("https://example.com/edital.pdf");
        });
    }

    @Test
    @DisplayName("should prefer original edital over retificacao when both are present")
    void shouldPreferOriginalEditalOverRetificacaoWhenBothArePresent() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">EDITAL 007/2026 - PROCESSO SELETIVO 007/2026 - CONTRATACAO DE PROFESSOR</p>
                    <p><a href="https://example.com/retificacao.pdf">Retificação do Edital 007/2026</a></p>
                    <p><a href="https://example.com/edital-original.pdf">Edital 007/2026 - Processo Seletivo 007/2026</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.items()).singleElement().satisfies(item -> {
            assertThat(item.editalUrl()).isEqualTo("https://example.com/edital-original.pdf");
            assertThat(item.contestUrl()).isEqualTo("https://example.com/edital-original.pdf");
        });
    }

    @Test
    @DisplayName("should accept common retificacao label variants when no original edital is present")
    void shouldAcceptCommonRetificacaoLabelVariantsWhenNoOriginalEditalIsPresent() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">EDITAL 009/2026 - PROCESSO SELETIVO 009/2026 - CONTRATACAO DE PROFESSOR</p>
                    <p><a href="https://example.com/retificado.pdf">Edital retificado 009/2026</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.items()).singleElement().satisfies(item -> {
            assertThat(item.editalUrl()).isEqualTo("https://example.com/retificado.pdf");
            assertThat(item.contestUrl()).isEqualTo("https://example.com/retificado.pdf");
        });
    }

    @Test
    @DisplayName("should accept short retificacao labels when they are the only edital available")
    void shouldAcceptShortRetificacaoLabelsWhenTheyAreTheOnlyEditalAvailable() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">EDITAL 010/2026 - PROCESSO SELETIVO 010/2026 - CONTRATACAO DE PROFESSOR</p>
                    <p><a href="https://example.com/retificacao-curta.pdf">1ª Retificação</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.items()).singleElement().satisfies(item -> {
            assertThat(item.editalUrl()).isEqualTo("https://example.com/retificacao-curta.pdf");
            assertThat(item.contestUrl()).isEqualTo("https://example.com/retificacao-curta.pdf");
            assertThat(item.editalYear()).isEqualTo(2026);
        });
    }

    @Test
    @DisplayName("should skip contest blocks when no edital style attachment is present")
    void shouldSkipContestBlocksWhenNoEditalStyleAttachmentIsPresent() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">EDITAL 004/2026 - PROCESSO SELETIVO 004/2026 - CONTRATACAO DE PROFESSOR</p>
                    <p>Sem anexos publicados ainda</p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.itemsFound()).isZero();
    }

    @Test
    @DisplayName("should exclude follow up blocks from becoming standalone contests")
    void shouldExcludeFollowUpBlocksFromBecomingStandaloneContests() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">RESULTADO FINAL DO PROCESSO SELETIVO 011/2026</p>
                    <p><a href="https://example.com/resultado-final.pdf">Resultado final do processo seletivo 011/2026</a></p>
                    <p class="contest-title">RETIFICACAO DO CONCURSO 011/2026</p>
                    <p><a href="https://example.com/retificacao.pdf">Retificação 011/2026</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.itemsFound()).isZero();
        assertThat(actual.items()).isEmpty();
    }

    @Test
    @DisplayName("should exclude numbered retificacao titles from contest extraction")
    void shouldExcludeNumberedRetificacaoTitlesFromContestExtraction() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE EDUCACAO</p>
                    <p class="contest-title">1ª Retificação do Processo Seletivo 011/2026</p>
                    <p><a href="https://example.com/retificacao-011.pdf">Retificação 011/2026</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.itemsFound()).isZero();
        assertThat(actual.items()).isEmpty();
    }

    @Test
    @DisplayName("should accept plain edital headings when they expose a valid edital attachment")
    void shouldAcceptPlainEditalHeadingsWhenTheyExposeAValidEditalAttachment() {
        InconfidentesEditaisFixtureParser parser = new InconfidentesEditaisFixtureParser();

        String html = """
                <html><body><div class="entry-content">
                    <p class="department">DEPARTAMENTO DE ADMINISTRACAO</p>
                    <p class="contest-title">EDITAL 015/2026</p>
                    <p><a href="https://example.com/edital-015.pdf">Edital 015/2026</a></p>
                </div></body></html>
                """;

        InconfidentesParsePreview actual = parser.parse(html, "https://inconfidentes.mg.gov.br/editais");

        assertThat(actual.items()).singleElement().satisfies(item -> {
            assertThat(item.contestTitle()).isEqualTo("EDITAL 015/2026");
            assertThat(item.editalUrl()).isEqualTo("https://example.com/edital-015.pdf");
            assertThat(item.editalYear()).isEqualTo(2026);
        });
    }

    private String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
