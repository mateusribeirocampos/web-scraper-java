package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.PciConcursosContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.PciConcursosFixtureParser;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PciConcursosScraperStrategy.
 *
 * TDD RED: written before the strategy exists.
 */
@Tag("unit")
@DisplayName("PciConcursosScraperStrategy")
class PciConcursosScraperStrategyTest {

    @Test
    @DisplayName("should support PCI Concursos static html sites using explicit metadata")
    void shouldSupportPciConcursosStaticHtmlSitesUsingExplicitMetadata() {
        PciConcursosScraperStrategy strategy = new PciConcursosScraperStrategy(
                new FakeJobFetcher(Map.of()),
                new PciConcursosFixtureParser(),
                new PciConcursosContestNormalizer()
        );

        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("pci_concursos")
                .displayName("PCI Concursos")
                .baseUrl("https://www.pciconcursos.com.br")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("pci_concursos_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-12T18:00:00Z"))
                .build();

        assertThat(strategy.supports(site)).isTrue();
    }

    @Test
    @DisplayName("should paginate pci fixture pages and return normalized contest postings")
    void shouldPaginatePciFixturePagesAndReturnNormalizedContestPostings() throws IOException {
        Map<String, FetchedPage> pages = new LinkedHashMap<>();
        pages.put(
                "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao",
                fetchedPage(
                        "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao",
                        fixture("fixtures/pci/pci-concursos-page-1.html"),
                        LocalDateTime.parse("2026-03-12T18:30:00")
                )
        );
        pages.put(
                "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao/pagina-2",
                fetchedPage(
                        "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao/pagina-2",
                        fixture("fixtures/pci/pci-concursos-page-2.html"),
                        LocalDateTime.parse("2026-03-12T18:31:00")
                )
        );

        PciConcursosScraperStrategy strategy = new PciConcursosScraperStrategy(
                new FakeJobFetcher(pages),
                new PciConcursosFixtureParser(),
                new PciConcursosContestNormalizer()
        );

        ScrapeCommand command = new ScrapeCommand(
                "pci_concursos",
                "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(command);

        assertThat(result.success()).isTrue();
        assertThat(result.source()).isEqualTo("pci_concursos");
        assertThat(result.items()).hasSize(3);
        assertThat(result.items()).extracting(PublicContestPostingEntity::getContestName)
                .containsExactly(
                        "Prefeitura de Curitiba abre processo seletivo para Desenvolvedor Java",
                        "Fundacao Digital SP lanca edital para Analista de Infraestrutura",
                        "Tribunal de Justica do Estado do Parana abre concurso para Especialista em Dados"
                );
        assertThat(result.items().get(0).getGovernmentLevel().name()).isEqualTo("MUNICIPAL");
        assertThat(result.items().get(1).getGovernmentLevel().name()).isEqualTo("AUTARCHY");
        assertThat(result.items().get(2).getGovernmentLevel().name()).isEqualTo("ESTADUAL");
        assertThat(result.items().get(0).getBaseSalary()).isNotNull();
        assertThat(result.items().get(0).getExternalId())
                .isEqualTo("https://www.pciconcursos.com.br/concurso/prefeitura-de-curitiba-pr-abre-processo-seletivo-para-desenvolvedor-java");
        assertThat(result.items().get(0).getPublishedAt()).isEqualTo(java.time.LocalDate.parse("2026-03-12"));
    }

    @Test
    @DisplayName("should stop pagination when next page resolves to the current page again")
    void shouldStopPaginationWhenNextPageResolvesToTheCurrentPageAgain() {
        AtomicInteger fetchCount = new AtomicInteger();
        JobFetcher selfReferentialFetcher = request -> {
            fetchCount.incrementAndGet();
            String html = """
                    <html><body>
                    <main class="listagem-concursos">
                        <article class="ca">
                            <a class="ca-link" href="/concurso/teste">Concurso teste</a>
                            <span class="ca-orgao">Prefeitura Municipal de Teste</span>
                            <h2 class="ca-cargo">Analista</h2>
                            <li class="ca-escolaridade">Nivel superior</li>
                            <li class="ca-salario">R$ 4.500,00</li>
                            <li class="ca-inscricoes">Inscricoes de 2026-03-20 ate 2026-04-10</li>
                            <a class="ca-detalhes" href="/concurso/teste/edital">Ver edital</a>
                        </article>
                    </main>
                    <nav class="pagination">
                        <a class="next" href="">Proxima</a>
                    </nav>
                    </body></html>
                    """;
            return new FetchedPage(request.url(), html, 200, "text/html", LocalDateTime.parse("2026-03-12T18:30:00"));
        };

        PciConcursosScraperStrategy strategy = new PciConcursosScraperStrategy(
                selfReferentialFetcher,
                new PciConcursosFixtureParser(),
                new PciConcursosContestNormalizer()
        );

        ScrapeResult<PublicContestPostingEntity> result = strategy.scrape(new ScrapeCommand(
                "pci_concursos",
                "https://www.pciconcursos.com.br/concursos/tecnologia-da-informacao",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(fetchCount.get()).isEqualTo(1);
    }

    private static FetchedPage fetchedPage(String url, String html, LocalDateTime fetchedAt) {
        return new FetchedPage(url, html, 200, "text/html", fetchedAt);
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = PciConcursosScraperStrategyTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class FakeJobFetcher implements JobFetcher {

        private final Map<String, FetchedPage> pagesByUrl;

        private FakeJobFetcher(Map<String, FetchedPage> pagesByUrl) {
            this.pagesByUrl = pagesByUrl;
        }

        @Override
        public FetchedPage fetch(FetchRequest request) {
            FetchedPage page = pagesByUrl.get(request.url());
            if (page == null) {
                throw new IllegalStateException("No fixture configured for url=" + request.url());
            }
            return page;
        }
    }
}
