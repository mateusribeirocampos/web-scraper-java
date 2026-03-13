package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.PciConcursosPreviewItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PCI Concursos contest normalization rules.
 */
@Tag("unit")
@DisplayName("PciConcursosContestNormalizer")
class PciConcursosContestNormalizerTest {

    @Test
    @DisplayName("should keep parsing when salary description contains a range")
    void shouldKeepParsingWhenSalaryDescriptionContainsARange() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Universidade Federal abre concurso para Cientista de Dados",
                "Universidade Federal do Parana",
                "Cientista de Dados",
                3,
                "SUPERIOR",
                "R$ 6.200,50 a R$ 8.000,00",
                "2026-03-15",
                "2026-04-10",
                "https://www.pciconcursos.com.br/concurso/ufpr",
                "https://www.pciconcursos.com.br/concurso/ufpr-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-12T18:30:00"));

        assertThat(posting.getBaseSalary()).isEqualByComparingTo(new BigDecimal("6200.50"));
        assertThat(posting.getSalaryDescription()).isEqualTo("R$ 6.200,50 a R$ 8.000,00");
        assertThat(posting.getExternalId()).isEqualTo("https://www.pciconcursos.com.br/concurso/ufpr");
        assertThat(posting.getCanonicalUrl()).isEqualTo("https://www.pciconcursos.com.br/concurso/ufpr");
        assertThat(posting.getEditalUrl()).isEqualTo("https://www.pciconcursos.com.br/concurso/ufpr-edital");
        assertThat(posting.getPublishedAt()).isEqualTo(java.time.LocalDate.parse("2026-03-12"));
    }

    @Test
    @DisplayName("should parse thousand separated salaries without centavos")
    void shouldParseThousandSeparatedSalariesWithoutCentavos() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Concurso com salario sem centavos",
                "Prefeitura Municipal de Teste",
                "Analista",
                2,
                "SUPERIOR",
                "R$ 1.000 a R$ 2.000",
                "2026-03-15",
                "2026-04-10",
                "https://www.pciconcursos.com.br/concurso/prefeitura",
                "https://www.pciconcursos.com.br/concurso/prefeitura-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-12T18:30:00"));

        assertThat(posting.getBaseSalary()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("should parse 4 digit salaries without thousand separators")
    void shouldParseFourDigitSalariesWithoutThousandSeparators() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Concurso com salario simples",
                "Prefeitura Municipal de Teste",
                "Analista",
                2,
                "SUPERIOR",
                "R$ 1500,00",
                "2026-03-15",
                "2026-04-10",
                "https://www.pciconcursos.com.br/concurso/prefeitura",
                "https://www.pciconcursos.com.br/concurso/prefeitura-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-12T18:30:00"));

        assertThat(posting.getBaseSalary()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("should ignore non salary numbers that appear before the pay amount")
    void shouldIgnoreNonSalaryNumbersThatAppearBeforeThePayAmount() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Concurso com jornada e salario",
                "Prefeitura Municipal de Teste",
                "Analista",
                2,
                "SUPERIOR",
                "40h semanais, R$ 2.500,00",
                "2026-03-15",
                "2026-04-10",
                "https://www.pciconcursos.com.br/concurso/prefeitura",
                "https://www.pciconcursos.com.br/concurso/prefeitura-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-12T18:30:00"));

        assertThat(posting.getBaseSalary()).isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("should classify obvious federal organizers as federal")
    void shouldClassifyObviousFederalOrganizersAsFederal() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Ministerio da Fazenda abre concurso para Engenheiro de Plataforma",
                "Instituto Federal de Brasilia",
                "Engenheiro de Plataforma",
                8,
                "POS_GRADUACAO",
                "R$ 12.500,00",
                "2026-03-18",
                "2026-04-18",
                "https://www.pciconcursos.com.br/concurso/ifb",
                "https://www.pciconcursos.com.br/concurso/ifb-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-12T18:30:00"));

        assertThat(posting.getGovernmentLevel().name()).isEqualTo("FEDERAL");
    }

    @Test
    @DisplayName("should default unknown education level to superior for persistence safety")
    void shouldDefaultUnknownEducationLevelToSuperiorForPersistenceSafety() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Concurso sem escolaridade definida",
                "Fundação de Apoio à Pesquisa",
                "Analista",
                1,
                null,
                "R$ 3.500,00",
                "2026-03-18",
                "2026-04-18",
                "https://www.pciconcursos.com.br/concurso/fap",
                "https://www.pciconcursos.com.br/concurso/fap-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-12T18:30:00"));

        assertThat(posting.getEducationLevel().name()).isEqualTo("SUPERIOR");
    }

    @Test
    @DisplayName("should normalize accents before inferring government level")
    void shouldNormalizeAccentsBeforeInferringGovernmentLevel() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Tribunal de Justiça do Estado abre concurso para analista",
                "Fundação de Justiça do Estado",
                "Analista",
                4,
                "SUPERIOR",
                "R$ 7.800,00",
                "2026-03-18",
                "2026-04-18",
                "https://www.pciconcursos.com.br/concurso/tj",
                "https://www.pciconcursos.com.br/concurso/tj-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-03-12T18:30:00"));

        assertThat(posting.getGovernmentLevel().name()).isEqualTo("ESTADUAL");
    }

    @Test
    @DisplayName("should mark contest as registration closed when fetch date is after deadline")
    void shouldMarkContestAsRegistrationClosedWhenFetchDateIsAfterDeadline() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Concurso ainda listado apos prazo",
                "Prefeitura Municipal de Teste",
                "Analista",
                2,
                "SUPERIOR",
                "R$ 4.500,00",
                "2026-03-15",
                "2026-04-10",
                "https://www.pciconcursos.com.br/concurso/prefeitura",
                "https://www.pciconcursos.com.br/concurso/prefeitura-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-05-01T10:00:00"));

        assertThat(posting.getContestStatus().name()).isEqualTo("REGISTRATION_CLOSED");
    }

    @Test
    @DisplayName("should keep normalizing when registration dates are missing")
    void shouldKeepNormalizingWhenRegistrationDatesAreMissing() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Concurso sem periodo informado",
                "Prefeitura Municipal de Teste",
                "Analista",
                2,
                "SUPERIOR",
                "R$ 4.500,00",
                null,
                null,
                "https://www.pciconcursos.com.br/concurso/prefeitura",
                "https://www.pciconcursos.com.br/concurso/prefeitura-edital"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-05-01T10:00:00"));

        assertThat(posting.getPublishedAt()).isEqualTo(java.time.LocalDate.parse("2026-05-01"));
        assertThat(posting.getContestStatus().name()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("should keep publishedAt on crawl date when registrations start in the future")
    void shouldKeepPublishedAtOnCrawlDateWhenRegistrationsStartInTheFuture() {
        PciConcursosContestNormalizer normalizer = new PciConcursosContestNormalizer();

        PciConcursosPreviewItem item = new PciConcursosPreviewItem(
                "Concurso anunciado antes das inscricoes",
                "Instituto Federal de Teste",
                "Analista",
                2,
                "SUPERIOR",
                "R$ 4.500,00",
                "2026-04-20",
                "2026-05-20",
                "https://www.pciconcursos.com.br/concurso/ift",
                "https://www.pciconcursos.com.br/concurso/ift-edital-v2"
        );

        PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.parse("2026-04-10T10:00:00"));

        assertThat(posting.getPublishedAt()).isEqualTo(java.time.LocalDate.parse("2026-04-10"));
        assertThat(posting.getExternalId()).isEqualTo("https://www.pciconcursos.com.br/concurso/ift");
        assertThat(posting.getEditalUrl()).isEqualTo("https://www.pciconcursos.com.br/concurso/ift-edital-v2");
    }
}
