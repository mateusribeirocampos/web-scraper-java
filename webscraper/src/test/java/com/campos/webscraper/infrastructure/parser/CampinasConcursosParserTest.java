package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CampinasConcursosParser")
class CampinasConcursosParserTest {

    private final CampinasConcursosParser parser = new CampinasConcursosParser();

    @Test
    @DisplayName("should parse active official Campinas alert from JSONAPI payload")
    void shouldParseActiveOfficialCampinasAlertFromJsonApiPayload() {
        assertThat(parser.parse(activeJson(), sourceUrl(), LocalDateTime.parse("2026-03-31T10:15:00")))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.contestTitle()).contains("Analista de Sistemas");
                    assertThat(item.organizer()).isEqualTo("Prefeitura Municipal de Campinas");
                    assertThat(item.positionTitle()).isEqualTo("Analista de Sistemas");
                    assertThat(item.contestCode()).isEqualTo("PCAM2601");
                    assertThat(item.officialSiteUrl()).isEqualTo("https://campinas.sp.gov.br/sites/concursos/");
                    assertThat(item.sourceApiUrl()).isEqualTo(sourceUrl());
                    assertThat(item.editalUrl()).isEqualTo("https://www.vunesp.com.br/PCAM2601");
                    assertThat(item.publishedAt()).isEqualTo(LocalDate.parse("2026-03-19"));
                    assertThat(item.registrationStartDate()).isEqualTo(LocalDate.parse("2026-03-20"));
                    assertThat(item.registrationEndDate()).isEqualTo(LocalDate.parse("2026-04-10"));
                });
    }

    @Test
    @DisplayName("should keep contest even when alert text does not expose a parsable position pattern")
    void shouldKeepContestEvenWhenAlertTextDoesNotExposeAParsablePositionPattern() {
        assertThat(parser.parse(unmatchedTitleJson(), sourceUrl(), LocalDateTime.parse("2026-03-31T10:15:00")))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.contestTitle()).isEqualTo("Inscrições abertas do concurso público 2026");
                    assertThat(item.positionTitle()).isNull();
                    assertThat(item.registrationStartDate()).isNull();
                    assertThat(item.registrationEndDate()).isNull();
                });
    }

    @Test
    @DisplayName("should preserve active alert window when worker timezone differs from Campinas")
    void shouldPreserveActiveAlertWindowWhenWorkerTimezoneDiffersFromCampinas() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            assertThat(parser.parse(activeUntilLateNightJson(), sourceUrl(), LocalDateTime.parse("2026-04-11T01:30:00")))
                    .singleElement()
                    .satisfies(item -> {
                        assertThat(item.contestCode()).isEqualTo("PCAM2603");
                        assertThat(item.registrationEndDate()).isEqualTo(LocalDate.parse("2026-04-10"));
                    });
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("should derive stable identity from generic alert link instead of mutable title")
    void shouldDeriveStableIdentityFromGenericAlertLinkInsteadOfMutableTitle() {
        CampinasContestPreviewItem first = parser.parse(genericLinkJson(
                "Inscrições abertas para Analista de Sistemas de 20/03/26 a 10/04/26"
        ), sourceUrl(), LocalDateTime.parse("2026-03-31T10:15:00")).getFirst();

        CampinasContestPreviewItem second = parser.parse(genericLinkJson(
                "Inscrições prorrogadas para Analista de Sistemas de 20/03/26 a 17/04/26"
        ), sourceUrl(), LocalDateTime.parse("2026-04-01T10:15:00")).getFirst();

        assertThat(first.contestCode()).isEqualTo("campinas-sp-gov-br-sites-concursos-edital-analista-de-sistemas-pdf");
        assertThat(second.contestCode()).isEqualTo(first.contestCode());
    }

    @Test
    @DisplayName("should parse Drupal link object for official Campinas alert")
    void shouldParseDrupalLinkObjectForOfficialCampinasAlert() {
        assertThat(parser.parse(drupalLinkObjectJson(), sourceUrl(), LocalDateTime.parse("2026-03-31T10:15:00")))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.contestCode()).isEqualTo("PCAM2604");
                    assertThat(item.editalUrl()).isEqualTo("https://www.vunesp.com.br/PCAM2604");
                    assertThat(item.registrationEndDate()).isEqualTo(LocalDate.parse("2026-04-15"));
                });
    }

    @Test
    @DisplayName("should resolve Drupal internal links to absolute Campinas URLs")
    void shouldResolveDrupalInternalLinksToAbsoluteCampinasUrls() {
        assertThat(parser.parse(drupalInternalLinkJson(), sourceUrl(), LocalDateTime.parse("2026-03-31T10:15:00")))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.contestCode()).isEqualTo("campinas-sp-gov-br-sites-concursos-edital-tecnologia-pdf");
                    assertThat(item.editalUrl()).isEqualTo("https://campinas.sp.gov.br/sites/concursos/edital-tecnologia.pdf");
                });
    }

    @Test
    @DisplayName("should resolve relative links to the same stable Campinas absolute URL")
    void shouldResolveRelativeLinksToTheSameStableCampinasAbsoluteUrl() {
        CampinasContestPreviewItem internalLinkItem = parser.parse(
                drupalInternalLinkJson(),
                sourceUrl(),
                LocalDateTime.parse("2026-03-31T10:15:00")
        ).getFirst();

        CampinasContestPreviewItem relativeLinkItem = parser.parse(
                relativeLinkJson(),
                sourceUrl(),
                LocalDateTime.parse("2026-03-31T10:15:00")
        ).getFirst();

        assertThat(relativeLinkItem.editalUrl()).isEqualTo(internalLinkItem.editalUrl());
        assertThat(relativeLinkItem.contestCode()).isEqualTo(internalLinkItem.contestCode());
    }

    @Test
    @DisplayName("should normalize textual Drupal internal links like object links")
    void shouldNormalizeTextualDrupalInternalLinksLikeObjectLinks() {
        CampinasContestPreviewItem textualInternalItem = parser.parse(
                textualInternalLinkJson(),
                sourceUrl(),
                LocalDateTime.parse("2026-03-31T10:15:00")
        ).getFirst();

        CampinasContestPreviewItem objectInternalItem = parser.parse(
                drupalInternalLinkJson(),
                sourceUrl(),
                LocalDateTime.parse("2026-03-31T10:15:00")
        ).getFirst();

        assertThat(textualInternalItem.editalUrl()).isEqualTo(objectInternalItem.editalUrl());
        assertThat(textualInternalItem.contestCode()).isEqualTo(objectInternalItem.contestCode());
    }

    @Test
    @DisplayName("should ignore expired Campinas alert even when JSON still exposes it")
    void shouldIgnoreExpiredCampinasAlertEvenWhenJsonStillExposesIt() {
        assertThat(parser.parse(expiredJson(), sourceUrl(), LocalDateTime.parse("2026-03-31T10:15:00")))
                .isEmpty();
    }

    private static String sourceUrl() {
        return "https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658";
    }

    private static String activeJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-19T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições abertas para Analista de Sistemas de 20/03/26 a 10/04/26",
                        "field_site_alerta_link": "https://www.vunesp.com.br/PCAM2601",
                        "field_site_alerta_exibicao": {
                          "value": "2026-03-20T00:00:00-03:00",
                          "end_value": "2026-04-10T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }

    private static String expiredJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2025-12-04T08:00:00-03:00",
                        "field_site_alerta_titulo": "Reabertas as inscrições para Engenheiro de Segurança do Trabalho de 05/12/25 a 15/01/26",
                        "field_site_alerta_link": "https://www.vunesp.com.br/PCAM2501",
                        "field_site_alerta_exibicao": {
                          "value": "2025-12-05T00:00:00-03:00",
                          "end_value": "2026-01-15T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }

    private static String unmatchedTitleJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-19T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições abertas do concurso público 2026",
                        "field_site_alerta_link": "https://www.vunesp.com.br/PCAM2602",
                        "field_site_alerta_exibicao": {
                          "value": "2026-03-20T00:00:00-03:00",
                          "end_value": "2026-04-10T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }

    private static String activeUntilLateNightJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-04-01T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições abertas para Desenvolvedor de 01/04/26 a 10/04/26",
                        "field_site_alerta_link": "https://www.vunesp.com.br/PCAM2603",
                        "field_site_alerta_exibicao": {
                          "value": "2026-04-01T00:00:00-03:00",
                          "end_value": "2026-04-10T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }

    private static String genericLinkJson(String alertTitle) {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-19T08:00:00-03:00",
                        "field_site_alerta_titulo": "%s",
                        "field_site_alerta_link": "https://campinas.sp.gov.br/sites/concursos/edital-analista-de-sistemas.pdf",
                        "field_site_alerta_exibicao": {
                          "value": "2026-03-20T00:00:00-03:00",
                          "end_value": "2026-04-17T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """.formatted(alertTitle);
    }

    private static String drupalLinkObjectJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-25T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições abertas para Arquiteto de 25/03/26 a 15/04/26",
                        "field_site_alerta_link": {
                          "uri": "https://www.vunesp.com.br/PCAM2604",
                          "url": "https://www.vunesp.com.br/PCAM2604"
                        },
                        "field_site_alerta_exibicao": {
                          "value": "2026-03-25T00:00:00-03:00",
                          "end_value": "2026-04-15T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }

    private static String drupalInternalLinkJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-25T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições abertas para Tecnologia de 25/03/26 a 15/04/26",
                        "field_site_alerta_link": {
                          "uri": "internal:/sites/concursos/edital-tecnologia.pdf"
                        },
                        "field_site_alerta_exibicao": {
                          "value": "2026-03-25T00:00:00-03:00",
                          "end_value": "2026-04-15T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }

    private static String relativeLinkJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-25T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições prorrogadas para Tecnologia de 25/03/26 a 18/04/26",
                        "field_site_alerta_link": {
                          "uri": "/sites/concursos/edital-tecnologia.pdf"
                        },
                        "field_site_alerta_exibicao": {
                          "value": "2026-03-25T00:00:00-03:00",
                          "end_value": "2026-04-18T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }

    private static String textualInternalLinkJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-25T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições abertas para Tecnologia de 25/03/26 a 15/04/26",
                        "field_site_alerta_link": "internal:/sites/concursos/edital-tecnologia.pdf",
                        "field_site_alerta_exibicao": {
                          "value": "2026-03-25T00:00:00-03:00",
                          "end_value": "2026-04-15T23:00:00-03:00"
                        }
                      }
                    }
                  ]
                }
                """;
    }
}
