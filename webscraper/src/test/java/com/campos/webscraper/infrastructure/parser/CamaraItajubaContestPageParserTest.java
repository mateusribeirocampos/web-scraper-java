package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CamaraItajubaContestPageParser")
class CamaraItajubaContestPageParserTest {

    private final CamaraItajubaContestPageParser parser = new CamaraItajubaContestPageParser();

    @Test
    @DisplayName("should parse Câmara Itajubá contest page into one canonical contest preview")
    void shouldParseCamaraItajubaContestPageIntoOneCanonicalContestPreview() throws Exception {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";

        assertThat(parser.parse(fixture("fixtures/camara-itajuba/camara-itajuba-launch-page.html"), sourceUrl))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.contestTitle()).isEqualTo("Concurso Público 2023");
                    assertThat(item.organizer()).isEqualTo("Câmara Municipal de Itajubá");
                    assertThat(item.positionTitle()).isEqualTo("Cargos efetivos diversos");
                    assertThat(item.editalUrl()).isEqualTo("https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf");
                    assertThat(item.publishedAt()).isEqualTo(LocalDate.parse("2023-12-13"));
                    assertThat(item.registrationStartDate()).isEqualTo(LocalDate.parse("2024-02-01"));
                    assertThat(item.registrationEndDate()).isNull();
                    assertThat(item.examDate()).isNull();
                    assertThat(item.numberOfVacancies()).isEqualTo(12);
                    assertThat(item.salaryDescription()).isBlank();
                    assertThat(item.attachments()).hasSize(1);
                });
    }

    @Test
    @DisplayName("should identify edital by anchor label even when pdf url does not contain edital")
    void shouldIdentifyEditalByAnchorLabelEvenWhenPdfUrlDoesNotContainEdital() {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <title>Concurso Público 2023 - Câmara Municipal de Itajubá</title>
                  <meta property="article:published_time" content="2023-12-13T14:58:47+00:00" />
                </head>
                <body>
                  <h1 class="elementor-heading-title">Concurso Público 2023</h1>
                  <div class="elementor-widget-theme-post-content">
                    <p>As inscrições estão previstas para começar em fevereiro de 2024.</p>
                    <p>As provas estão programadas para o mês de maio 2024.</p>
                    <p><a href="/site/wp-content/uploads/2024/01/concurso-publico.pdf">PDF do Edital</a></p>
                  </div>
                </body>
                </html>
                """;

        assertThat(parser.parse(html, sourceUrl))
                .singleElement()
                .satisfies(item -> assertThat(item.editalUrl())
                        .isEqualTo("https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2024/01/concurso-publico.pdf"));
    }

    @Test
    @DisplayName("should prefer original edital over follow up pdf attachments")
    void shouldPreferOriginalEditalOverFollowUpPdfAttachments() {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <title>Concurso Público 2023 - Câmara Municipal de Itajubá</title>
                  <meta property="article:published_time" content="2023-12-13T14:58:47+00:00" />
                </head>
                <body>
                  <h1 class="elementor-heading-title">Concurso Público 2023</h1>
                  <div class="elementor-widget-theme-post-content">
                    <p>As inscrições estão previstas para começar em fevereiro de 2024.</p>
                    <p>As provas estão programadas para o mês de maio 2024.</p>
                    <p><a href="/site/wp-content/uploads/2024/02/retificacao-do-edital.pdf">Retificação do Edital</a></p>
                    <p><a href="/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf">PDF do Edital</a></p>
                  </div>
                </body>
                </html>
                """;

        assertThat(parser.parse(html, sourceUrl))
                .singleElement()
                .satisfies(item -> assertThat(item.editalUrl())
                        .isEqualTo("https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf"));
    }

    @Test
    @DisplayName("should not derive schedule year from later update years elsewhere on page")
    void shouldNotDeriveScheduleYearFromLaterUpdateYearsElsewhereOnPage() {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <title>Concurso Público 2023 - Câmara Municipal de Itajubá</title>
                  <meta property="article:published_time" content="2024-02-10T12:00:00+00:00" />
                </head>
                <body>
                  <h1 class="elementor-heading-title">Concurso Público 2023</h1>
                  <div class="elementor-widget-theme-post-content">
                    <p>As inscrições estão previstas para começar em fevereiro de 2024.</p>
                    <p>O prazo termina até o dia 29 de fevereiro.</p>
                    <p>As provas serão realizadas no dia 31 de maio.</p>
                    <p>Atualização institucional publicada em 2026 sobre arquivos históricos.</p>
                    <p><a href="/site/wp-content/uploads/2023/12/arquivo.pdf">PDF do Edital</a></p>
                  </div>
                </body>
                </html>
                """;

        assertThat(parser.parse(html, sourceUrl))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.registrationEndDate()).isEqualTo(LocalDate.parse("2024-02-29"));
                    assertThat(item.examDate()).isEqualTo(LocalDate.parse("2024-05-31"));
                });
    }

    @Test
    @DisplayName("should convert wordpress published time to Sao Paulo local date before truncating")
    void shouldConvertWordpressPublishedTimeToSaoPauloLocalDateBeforeTruncating() {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <title>Concurso Público 2023 - Câmara Municipal de Itajubá</title>
                  <meta property="article:published_time" content="2023-12-13T01:30:00+00:00" />
                </head>
                <body>
                  <h1 class="elementor-heading-title">Concurso Público 2023</h1>
                  <div class="elementor-widget-theme-post-content">
                    <p>As inscrições estão previstas para começar em fevereiro de 2024.</p>
                    <p><a href="/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf">PDF do Edital</a></p>
                  </div>
                </body>
                </html>
                """;

        assertThat(parser.parse(html, sourceUrl))
                .singleElement()
                .satisfies(item -> assertThat(item.publishedAt()).isEqualTo(LocalDate.parse("2023-12-12")));
    }

    @Test
    @DisplayName("should not derive leap day schedule from edital year when text omits stable year")
    void shouldNotDeriveLeapDayScheduleFromEditalYearWhenTextOmitsStableYear() {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <title>Concurso Público 2023 - Câmara Municipal de Itajubá</title>
                  <meta property="article:published_time" content="2026-02-10T12:00:00+00:00" />
                </head>
                <body>
                  <h1 class="elementor-heading-title">Concurso Público 2023</h1>
                  <div class="elementor-widget-theme-post-content">
                    <p>O prazo termina até o dia 29 de fevereiro.</p>
                    <p>As provas serão realizadas no dia 31 de maio.</p>
                    <p><a href="/site/wp-content/uploads/2024/01/EDITAL_CMI_2024.pdf">PDF do Edital</a></p>
                  </div>
                </body>
                </html>
                """;

        assertThat(parser.parse(html, sourceUrl))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.registrationEndDate()).isNull();
                    assertThat(item.examDate()).isNull();
                });
    }

    @Test
    @DisplayName("should not derive schedule year from later published metadata when edital url has no year")
    void shouldNotDeriveScheduleYearFromLaterPublishedMetadataWhenEditalUrlHasNoYear() {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <title>Concurso Público 2023 - Câmara Municipal de Itajubá</title>
                  <meta property="article:published_time" content="2026-02-10T12:00:00+00:00" />
                </head>
                <body>
                  <h1 class="elementor-heading-title">Concurso Público 2023</h1>
                  <div class="elementor-widget-theme-post-content">
                    <p>O prazo termina até o dia 29 de fevereiro.</p>
                    <p>As provas serão realizadas no dia 31 de maio.</p>
                    <p><a href="/site/wp-content/uploads/arquivo.pdf">PDF do Edital</a></p>
                  </div>
                </body>
                </html>
                """;

        assertThat(parser.parse(html, sourceUrl))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.registrationEndDate()).isNull();
                    assertThat(item.examDate()).isNull();
                });
    }

    @Test
    @DisplayName("should not derive schedule year from edital filename when schedule text omits year")
    void shouldNotDeriveScheduleYearFromEditalFilenameWhenScheduleTextOmitsYear() {
        String sourceUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <title>Concurso Público 2023 - Câmara Municipal de Itajubá</title>
                  <meta property="article:published_time" content="2023-12-13T14:58:47+00:00" />
                </head>
                <body>
                  <h1 class="elementor-heading-title">Concurso Público 2023</h1>
                  <div class="elementor-widget-theme-post-content">
                    <p>O prazo termina até o dia 29 de fevereiro.</p>
                    <p>As provas serão realizadas no dia 31 de maio.</p>
                    <p><a href="/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf">PDF do Edital</a></p>
                  </div>
                </body>
                </html>
                """;

        assertThat(parser.parse(html, sourceUrl))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.registrationEndDate()).isNull();
                    assertThat(item.examDate()).isNull();
                });
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = CamaraItajubaContestPageParserTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
