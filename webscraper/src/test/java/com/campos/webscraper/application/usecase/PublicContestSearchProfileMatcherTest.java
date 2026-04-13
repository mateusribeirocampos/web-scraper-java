package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.enums.PublicContestSearchProfile;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("PublicContestSearchProfileMatcher")
class PublicContestSearchProfileMatcherTest {

    private final PublicContestSearchProfileMatcher matcher = new PublicContestSearchProfileMatcher();

    // -- helper --

    private static PublicContestPostingEntity.PublicContestPostingEntityBuilder baseContest() {
        return PublicContestPostingEntity.builder()
                .contestName("Concurso SERPRO 2026")
                .organizer("SERPRO")
                .canonicalUrl("https://example.com/contest/1")
                .governmentLevel(GovernmentLevel.FEDERAL)
                .contestStatus(ContestStatus.OPEN)
                .fingerprintHash("abc123");
    }

    // ==================== TI_DEGREE_AND_ROLE ====================

    @Nested
    @DisplayName("TI_DEGREE_AND_ROLE profile")
    class TiDegreeAndRole {

        @Test
        @DisplayName("should accept contest requiring SUPERIOR education with IT position title")
        void shouldAcceptSuperiorWithItPosition() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de Tecnologia da Informação")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept contest with 'Ciência da Computação' in position or payload")
        void shouldAcceptContestWithCienciaDaComputacao() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de Sistemas")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .payloadJson("{\"formacao\": \"Ciência da Computação\"}")
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept contest for Técnico em Informática with TECNICO education")
        void shouldAcceptTecnicoEmInformatica() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Técnico em Informática")
                    .educationLevel(EducationLevel.TECNICO)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept contest for Desenvolvedor with SUPERIOR education")
        void shouldAcceptDesenvolvedor() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Desenvolvedor de Sistemas")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept contest for Programador")
        void shouldAcceptProgramador() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Programador de Computador")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept contest for Analista de Sistemas")
        void shouldAcceptAnalistaDeSistemas() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de Sistemas")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept plural 'Ciências da Computação' degree in payload")
        void shouldAcceptPluralCienciasDaComputacao() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .payloadJson("{\"formacao\": \"Ciências da Computação\"}")
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept Engenharia da Computação degree signal in payload")
        void shouldAcceptEngenhariaComputacaoInPayload() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .payloadJson("{\"formacao\": \"Engenharia da Computação\"}")
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept Analista de Segurança da Informação")
        void shouldAcceptAnalistaDeSegurancaDaInformacao() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de Segurança da Informação")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept Técnico em Telecomunicações")
        void shouldAcceptTecnicoEmTelecomunicacoes() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Técnico em Telecomunicações")
                    .educationLevel(EducationLevel.TECNICO)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should reject contest for Assistente Administrativo even with SUPERIOR education")
        void shouldRejectAssistenteAdministrativo() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Assistente Administrativo")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isFalse();
        }

        @Test
        @DisplayName("should reject contest requiring only FUNDAMENTAL education")
        void shouldRejectFundamentalEducation() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Auxiliar de Serviços Gerais")
                    .educationLevel(EducationLevel.FUNDAMENTAL)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isFalse();
        }

        @Test
        @DisplayName("should reject contest requiring MEDIO education without IT role signal")
        void shouldRejectMedioWithoutItRole() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Agente de Saúde")
                    .educationLevel(EducationLevel.MEDIO)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isFalse();
        }

        @Test
        @DisplayName("should accept MEDIO education when position has clear IT signal")
        void shouldAcceptMedioWithItPositionSignal() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Técnico de Nível Médio em Informática")
                    .educationLevel(EducationLevel.MEDIO)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should reject contest with UNKNOWN education level and no IT signals")
        void shouldRejectUnknownEducationWithoutItSignals() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Auxiliar Técnico")
                    .educationLevel(EducationLevel.UNKNOWN)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isFalse();
        }

        @Test
        @DisplayName("should accept contest with UNKNOWN education but IT position signal")
        void shouldAcceptUnknownEducationWithItPosition() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de Tecnologia da Informação")
                    .educationLevel(EducationLevel.UNKNOWN)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept contest with UNKNOWN education but IT degree signal in payload")
        void shouldAcceptUnknownEducationWithItDegreeInPayload() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Concurso Público Municipal")
                    .educationLevel(EducationLevel.UNKNOWN)
                    .payloadJson("{\"formacao\": \"Ciência da Computação\"}")
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept DBA position")
        void shouldAcceptDba() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Administrador de Banco de Dados")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }

        @Test
        @DisplayName("should accept Suporte Técnico position with TECNICO education")
        void shouldAcceptSuporteTecnico() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Técnico de Suporte em Informática")
                    .educationLevel(EducationLevel.TECNICO)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_DEGREE_AND_ROLE)).isTrue();
        }
    }

    // ==================== TI_ROLE_BROAD ====================

    @Nested
    @DisplayName("TI_ROLE_BROAD profile")
    class TiRoleBroad {

        @Test
        @DisplayName("should accept IT position even without specific degree info in payload")
        void shouldAcceptItPositionWithoutDegreeInfo() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de TI")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_ROLE_BROAD)).isTrue();
        }

        @Test
        @DisplayName("should accept TECNICO education with IT position")
        void shouldAcceptTecnicoWithItPosition() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Técnico em Processamento de Dados")
                    .educationLevel(EducationLevel.TECNICO)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_ROLE_BROAD)).isTrue();
        }

        @Test
        @DisplayName("should reject FUNDAMENTAL education even with IT-sounding title")
        void shouldRejectFundamentalEvenWithItTitle() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Auxiliar de Informática")
                    .educationLevel(EducationLevel.FUNDAMENTAL)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_ROLE_BROAD)).isFalse();
        }

        @Test
        @DisplayName("should accept UNKNOWN education with IT position signal")
        void shouldAcceptUnknownEducationWithItPositionBroad() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de Sistemas")
                    .educationLevel(EducationLevel.UNKNOWN)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_ROLE_BROAD)).isTrue();
        }

        @Test
        @DisplayName("should reject non-IT position with SUPERIOR education")
        void shouldRejectNonItPositionSuperior() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Contador")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.TI_ROLE_BROAD)).isFalse();
        }
    }

    // ==================== UNFILTERED ====================

    @Nested
    @DisplayName("UNFILTERED profile")
    class Unfiltered {

        @Test
        @DisplayName("should accept any contest regardless of education or position")
        void shouldAcceptAnyContest() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Auxiliar de Serviços Gerais")
                    .educationLevel(EducationLevel.FUNDAMENTAL)
                    .build();

            assertThat(matcher.matches(contest, PublicContestSearchProfile.UNFILTERED)).isTrue();
        }
    }

    // ==================== Null safety ====================

    @Nested
    @DisplayName("null safety")
    class NullSafety {

        @Test
        @DisplayName("should reject null contest")
        void shouldRejectNullContest() {
            assertThatThrownBy(() -> matcher.matches(null, PublicContestSearchProfile.TI_DEGREE_AND_ROLE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null profile")
        void shouldRejectNullProfile() {
            PublicContestPostingEntity contest = baseContest()
                    .positionTitle("Analista de TI")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .build();

            assertThatThrownBy(() -> matcher.matches(contest, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
