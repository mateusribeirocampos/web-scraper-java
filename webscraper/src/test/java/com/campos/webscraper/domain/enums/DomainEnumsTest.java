package com.campos.webscraper.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Testes de invariantes dos enums de domínio.
 *
 * <p>Esses testes garantem que:
 * <ul>
 *   <li>Os valores obrigatórios de cada enum estão presentes (contrato do domínio).</li>
 *   <li>A quantidade de constantes é conhecida — mudanças acidentais falham aqui primeiro.</li>
 *   <li>Nenhum enum tem constante órfã sem correspondência no domínio documentado (ADR005).</li>
 * </ul>
 *
 * <p>Ciclo TDD: estes testes foram escritos ANTES das classes de produção (fase RED).
 */
@Tag("unit")
@DisplayName("Domain Enums — invariantes de contrato")
class DomainEnumsTest {

    // =========================================================================
    // SiteType
    // =========================================================================

    @Nested
    @DisplayName("SiteType")
    class SiteTypeTests {

        @Test
        @DisplayName("deve ter exatamente 5 tipos de site (A até E)")
        void shouldHaveFiveSiteTypes() {
            assertThat(SiteType.values()).hasSize(5);
        }

        @Test
        @DisplayName("deve conter TYPE_A (HTML estático — jsoup)")
        void shouldContainTypeA() {
            assertThatCode(() -> SiteType.valueOf("TYPE_A")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter TYPE_B (HTML semi-dinâmico)")
        void shouldContainTypeB() {
            assertThatCode(() -> SiteType.valueOf("TYPE_B")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter TYPE_C (JS-heavy — Playwright)")
        void shouldContainTypeC() {
            assertThatCode(() -> SiteType.valueOf("TYPE_C")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter TYPE_D (autenticado — excluído fase 1)")
        void shouldContainTypeD() {
            assertThatCode(() -> SiteType.valueOf("TYPE_D")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter TYPE_E (API oficial — prioridade máxima)")
        void shouldContainTypeE() {
            assertThatCode(() -> SiteType.valueOf("TYPE_E")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // ExtractionMode
    // =========================================================================

    @Nested
    @DisplayName("ExtractionMode")
    class ExtractionModeTests {

        @Test
        @DisplayName("deve ter exatamente 4 modos de extração")
        void shouldHaveFourExtractionModes() {
            assertThat(ExtractionMode.values()).hasSize(4);
        }

        @Test
        @DisplayName("API é modo de extração válido")
        void shouldContainApi() {
            assertThatCode(() -> ExtractionMode.valueOf("API")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("STATIC_HTML é modo de extração válido")
        void shouldContainStaticHtml() {
            assertThatCode(() -> ExtractionMode.valueOf("STATIC_HTML")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("DYNAMIC_HTML é modo de extração válido")
        void shouldContainDynamicHtml() {
            assertThatCode(() -> ExtractionMode.valueOf("DYNAMIC_HTML")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("BROWSER_AUTOMATION é modo de extração válido (somente Tipo C)")
        void shouldContainBrowserAutomation() {
            assertThatCode(() -> ExtractionMode.valueOf("BROWSER_AUTOMATION")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // JobCategory
    // =========================================================================

    @Nested
    @DisplayName("JobCategory")
    class JobCategoryTests {

        @Test
        @DisplayName("deve ter exatamente 2 categorias (setor privado + concurso público)")
        void shouldHaveTwoCategories() {
            assertThat(JobCategory.values()).hasSize(2);
        }

        @Test
        @DisplayName("deve conter PRIVATE_SECTOR")
        void shouldContainPrivateSector() {
            assertThatCode(() -> JobCategory.valueOf("PRIVATE_SECTOR")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter PUBLIC_CONTEST")
        void shouldContainPublicContest() {
            assertThatCode(() -> JobCategory.valueOf("PUBLIC_CONTEST")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // LegalStatus
    // =========================================================================

    @Nested
    @DisplayName("LegalStatus")
    class LegalStatusTests {

        @Test
        @DisplayName("deve ter exatamente 3 status legais")
        void shouldHaveThreeLegalStatuses() {
            assertThat(LegalStatus.values()).hasSize(3);
        }

        @Test
        @DisplayName("deve conter APPROVED")
        void shouldContainApproved() {
            assertThatCode(() -> LegalStatus.valueOf("APPROVED")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter PENDING_REVIEW")
        void shouldContainPendingReview() {
            assertThatCode(() -> LegalStatus.valueOf("PENDING_REVIEW")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter SCRAPING_PROIBIDO")
        void shouldContainScrapingProibido() {
            assertThatCode(() -> LegalStatus.valueOf("SCRAPING_PROIBIDO")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // JobContractType
    // =========================================================================

    @Nested
    @DisplayName("JobContractType")
    class JobContractTypeTests {

        @Test
        @DisplayName("deve ter exatamente 6 tipos de contrato")
        void shouldHaveSixContractTypes() {
            assertThat(JobContractType.values()).hasSize(6);
        }

        @Test
        @DisplayName("deve conter UNKNOWN")
        void shouldContainUnknown() {
            assertThatCode(() -> JobContractType.valueOf("UNKNOWN")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter CLT")
        void shouldContainClt() {
            assertThatCode(() -> JobContractType.valueOf("CLT")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter PJ")
        void shouldContainPj() {
            assertThatCode(() -> JobContractType.valueOf("PJ")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter INTERNSHIP")
        void shouldContainInternship() {
            assertThatCode(() -> JobContractType.valueOf("INTERNSHIP")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter FREELANCE")
        void shouldContainFreelance() {
            assertThatCode(() -> JobContractType.valueOf("FREELANCE")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter TEMPORARY")
        void shouldContainTemporary() {
            assertThatCode(() -> JobContractType.valueOf("TEMPORARY")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // SeniorityLevel
    // =========================================================================

    @Nested
    @DisplayName("SeniorityLevel")
    class SeniorityLevelTests {

        @Test
        @DisplayName("deve ter exatamente 5 níveis de senioridade")
        void shouldHaveFiveSeniorityLevels() {
            assertThat(SeniorityLevel.values()).hasSize(5);
        }

        @Test
        @DisplayName("JUNIOR deve ser primeiro — é o foco do projeto")
        void juniorShouldBeFirst() {
            assertThat(SeniorityLevel.values()[0]).isEqualTo(SeniorityLevel.JUNIOR);
        }

        @Test
        @DisplayName("deve conter MID")
        void shouldContainMid() {
            assertThatCode(() -> SeniorityLevel.valueOf("MID")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter SENIOR")
        void shouldContainSenior() {
            assertThatCode(() -> SeniorityLevel.valueOf("SENIOR")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter LEAD")
        void shouldContainLead() {
            assertThatCode(() -> SeniorityLevel.valueOf("LEAD")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter INTERN")
        void shouldContainIntern() {
            assertThatCode(() -> SeniorityLevel.valueOf("INTERN")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // GovernmentLevel
    // =========================================================================

    @Nested
    @DisplayName("GovernmentLevel")
    class GovernmentLevelTests {

        @Test
        @DisplayName("deve ter exatamente 4 esferas governamentais")
        void shouldHaveFourGovernmentLevels() {
            assertThat(GovernmentLevel.values()).hasSize(4);
        }

        @Test
        @DisplayName("FEDERAL deve ser primeiro — foco inicial (DOU API)")
        void federalShouldBeFirst() {
            assertThat(GovernmentLevel.values()[0]).isEqualTo(GovernmentLevel.FEDERAL);
        }

        @Test
        @DisplayName("deve conter ESTADUAL")
        void shouldContainEstadual() {
            assertThatCode(() -> GovernmentLevel.valueOf("ESTADUAL")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter MUNICIPAL")
        void shouldContainMunicipal() {
            assertThatCode(() -> GovernmentLevel.valueOf("MUNICIPAL")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter AUTARCHY (autarquias e fundações)")
        void shouldContainAutarchy() {
            assertThatCode(() -> GovernmentLevel.valueOf("AUTARCHY")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // EducationLevel
    // =========================================================================

    @Nested
    @DisplayName("EducationLevel")
    class EducationLevelTests {

        @Test
        @DisplayName("deve ter exatamente 5 níveis de escolaridade")
        void shouldHaveFiveEducationLevels() {
            assertThat(EducationLevel.values()).hasSize(5);
        }

        @Test
        @DisplayName("deve conter FUNDAMENTAL")
        void shouldContainFundamental() {
            assertThatCode(() -> EducationLevel.valueOf("FUNDAMENTAL")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter MEDIO")
        void shouldContainMedio() {
            assertThatCode(() -> EducationLevel.valueOf("MEDIO")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter TECNICO")
        void shouldContainTecnico() {
            assertThatCode(() -> EducationLevel.valueOf("TECNICO")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter SUPERIOR")
        void shouldContainSuperior() {
            assertThatCode(() -> EducationLevel.valueOf("SUPERIOR")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter POS_GRADUACAO")
        void shouldContainPosGraduacao() {
            assertThatCode(() -> EducationLevel.valueOf("POS_GRADUACAO")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // ContestStatus
    // =========================================================================

    @Nested
    @DisplayName("ContestStatus")
    class ContestStatusTests {

        @Test
        @DisplayName("deve ter exatamente 5 status de concurso")
        void shouldHaveFiveContestStatuses() {
            assertThat(ContestStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("OPEN deve ser primeiro — concursos com inscrições abertas são prioridade")
        void openShouldBeFirst() {
            assertThat(ContestStatus.values()[0]).isEqualTo(ContestStatus.OPEN);
        }

        @Test
        @DisplayName("deve conter REGISTRATION_CLOSED")
        void shouldContainRegistrationClosed() {
            assertThatCode(() -> ContestStatus.valueOf("REGISTRATION_CLOSED")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter EXAM_SCHEDULED")
        void shouldContainExamScheduled() {
            assertThatCode(() -> ContestStatus.valueOf("EXAM_SCHEDULED")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter RESULT_PUBLISHED")
        void shouldContainResultPublished() {
            assertThatCode(() -> ContestStatus.valueOf("RESULT_PUBLISHED")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter CANCELLED")
        void shouldContainCancelled() {
            assertThatCode(() -> ContestStatus.valueOf("CANCELLED")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // DedupStatus
    // =========================================================================

    @Nested
    @DisplayName("DedupStatus")
    class DedupStatusTests {

        @Test
        @DisplayName("deve ter exatamente 3 status de deduplicação")
        void shouldHaveThreeDedupStatuses() {
            assertThat(DedupStatus.values()).hasSize(3);
        }

        @Test
        @DisplayName("NEW deve ser primeiro — vaga nova é o caso mais frequente")
        void newShouldBeFirst() {
            assertThat(DedupStatus.values()[0]).isEqualTo(DedupStatus.NEW);
        }

        @Test
        @DisplayName("deve conter DUPLICATE — fingerprint idêntico já existe na base")
        void shouldContainDuplicate() {
            assertThatCode(() -> DedupStatus.valueOf("DUPLICATE")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter UPDATED — mesma vaga com campo mutável alterado")
        void shouldContainUpdated() {
            assertThatCode(() -> DedupStatus.valueOf("UPDATED")).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // CrawlExecutionStatus
    // =========================================================================

    @Nested
    @DisplayName("CrawlExecutionStatus")
    class CrawlExecutionStatusTests {

        @Test
        @DisplayName("deve ter exatamente 5 status de execução")
        void shouldHaveFiveExecutionStatuses() {
            assertThat(CrawlExecutionStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("PENDING deve ser primeiro — estado inicial de toda execução")
        void pendingShouldBeFirst() {
            assertThat(CrawlExecutionStatus.values()[0]).isEqualTo(CrawlExecutionStatus.PENDING);
        }

        @Test
        @DisplayName("deve conter RUNNING — execução em andamento")
        void shouldContainRunning() {
            assertThatCode(() -> CrawlExecutionStatus.valueOf("RUNNING")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter SUCCEEDED — execução concluída com sucesso")
        void shouldContainSucceeded() {
            assertThatCode(() -> CrawlExecutionStatus.valueOf("SUCCEEDED")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter FAILED — falhou, mas ainda dentro da política de retry")
        void shouldContainFailed() {
            assertThatCode(() -> CrawlExecutionStatus.valueOf("FAILED")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve conter DEAD_LETTER — esgotou retentativas, movida para dead-letter")
        void shouldContainDeadLetter() {
            assertThatCode(() -> CrawlExecutionStatus.valueOf("DEAD_LETTER")).doesNotThrowAnyException();
        }
    }
}
