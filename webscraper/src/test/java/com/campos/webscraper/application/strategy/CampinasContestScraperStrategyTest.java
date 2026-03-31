package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.CampinasContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.CampinasConcursosParser;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CampinasContestScraperStrategy")
class CampinasContestScraperStrategyTest {

    @Test
    @DisplayName("should support official Campinas public contest API sites")
    void shouldSupportOfficialCampinasPublicContestApiSites() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of()),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("municipal_campinas")
                .displayName("Prefeitura de Campinas - Concursos")
                .baseUrl(sourceUrl())
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("campinas_jsonapi_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-31T00:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should scrape active Campinas official alert into normalized contest posting")
    void shouldScrapeActiveCampinasOfficialAlertIntoNormalizedContestPosting() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), activeJson(), 200, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getExternalId()).isEqualTo("municipal_campinas:pcam2601");
            assertThat(item.getCanonicalUrl()).isEqualTo("https://www.vunesp.com.br/PCAM2601");
            assertThat(item.getContestName()).contains("Analista de Sistemas");
            assertThat(item.getOrganizer()).isEqualTo("Prefeitura Municipal de Campinas");
            assertThat(item.getPositionTitle()).isEqualTo("Analista de Sistemas");
            assertThat(item.getState()).isEqualTo("SP");
            assertThat(item.getPublishedAt()).isEqualTo(LocalDate.parse("2026-03-19"));
            assertThat(item.getRegistrationStartDate()).isEqualTo(LocalDate.parse("2026-03-20"));
            assertThat(item.getRegistrationEndDate()).isEqualTo(LocalDate.parse("2026-04-10"));
            assertThat(item.getContestStatus().name()).isEqualTo("OPEN");
            assertThat(item.getPayloadJson()).contains("sourceApiUrl");
        });
    }

    @Test
    @DisplayName("should fall back to contest title when Campinas alert does not expose a parsable position title")
    void shouldFallBackToContestTitleWhenCampinasAlertDoesNotExposeAParsablePositionTitle() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), unmatchedTitleJson(), 200, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getContestName()).isEqualTo("Inscrições abertas do concurso público 2026");
            assertThat(item.getPositionTitle()).isEqualTo("Inscrições abertas do concurso público 2026");
            assertThat(item.getRegistrationStartDate()).isNull();
            assertThat(item.getRegistrationEndDate()).isNull();
            assertThat(item.getContestStatus().name()).isEqualTo("OPEN");
        });
    }

    @Test
    @DisplayName("should derive stable external id from generic official Campinas link")
    void shouldDeriveStableExternalIdFromGenericOfficialCampinasLink() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), genericLinkJson(), 200, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getExternalId())
                    .isEqualTo("municipal_campinas:campinas-sp-gov-br-sites-concursos-edital-analista-de-sistemas-pdf");
        });
    }

    @Test
    @DisplayName("should scrape Campinas alert when Drupal JSON API exposes link as object")
    void shouldScrapeCampinasAlertWhenDrupalJsonApiExposesLinkAsObject() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), drupalLinkObjectJson(), 200, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getExternalId()).isEqualTo("municipal_campinas:pcam2604");
            assertThat(item.getCanonicalUrl()).isEqualTo("https://www.vunesp.com.br/PCAM2604");
            assertThat(item.getRegistrationEndDate()).isEqualTo(LocalDate.parse("2026-04-15"));
        });
    }

    @Test
    @DisplayName("should normalize Drupal internal Campinas links to absolute canonical URLs")
    void shouldNormalizeDrupalInternalCampinasLinksToAbsoluteCanonicalUrls() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), drupalInternalLinkJson(), 200, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getCanonicalUrl()).isEqualTo("https://campinas.sp.gov.br/sites/concursos/edital-tecnologia.pdf");
            assertThat(item.getExternalId())
                    .isEqualTo("municipal_campinas:campinas-sp-gov-br-sites-concursos-edital-tecnologia-pdf");
        });
    }

    @Test
    @DisplayName("should normalize textual Drupal internal Campinas links to absolute canonical URLs")
    void shouldNormalizeTextualDrupalInternalCampinasLinksToAbsoluteCanonicalUrls() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), textualInternalLinkJson(), 200, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.getCanonicalUrl()).isEqualTo("https://campinas.sp.gov.br/sites/concursos/edital-tecnologia.pdf");
            assertThat(item.getExternalId())
                    .isEqualTo("municipal_campinas:campinas-sp-gov-br-sites-concursos-edital-tecnologia-pdf");
        });
    }

    @Test
    @DisplayName("should return empty success when Campinas alert is expired")
    void shouldReturnEmptySuccessWhenCampinasAlertIsExpired() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), expiredJson(), 200, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("should fail scrape when Campinas official JSON endpoint returns non 2xx")
    void shouldFailScrapeWhenCampinasOfficialJsonEndpointReturnsNon2xx() {
        CampinasContestScraperStrategy strategy = new CampinasContestScraperStrategy(
                new FakeJobFetcher(Map.of(
                        sourceUrl(),
                        new FetchedPage(sourceUrl(), "", 503, "application/json",
                                LocalDateTime.parse("2026-03-31T10:15:00"))
                )),
                new CampinasConcursosParser(),
                new CampinasContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "municipal_campinas",
                sourceUrl(),
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("status 503");
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

    private static String genericLinkJson() {
        return """
                {
                  "data": [
                    {
                      "attributes": {
                        "title": "Concursos Públicos e Processos Seletivos",
                        "field_blc_exibir_alerta": true,
                        "field_dt_publish_on": "2026-03-19T08:00:00-03:00",
                        "field_site_alerta_titulo": "Inscrições abertas para Analista de Sistemas de 20/03/26 a 10/04/26",
                        "field_site_alerta_link": "https://campinas.sp.gov.br/sites/concursos/edital-analista-de-sistemas.pdf",
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

    private record FakeJobFetcher(Map<String, FetchedPage> responses) implements JobFetcher {
        @Override
        public FetchedPage fetch(FetchRequest request) {
            FetchedPage page = responses.get(request.url());
            if (page == null) {
                throw new IllegalStateException("Unexpected URL requested: " + request.url());
            }
            return page;
        }
    }
}
