package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.interfaces.dto.DouApiItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DouContestNormalizer.
 *
 * TDD RED: written before the normalizer exists.
 */
@Tag("unit")
@DisplayName("DouContestNormalizer")
class DouContestNormalizerTest {

    @Test
    @DisplayName("should map DOU API item to PublicContestPostingEntity core fields")
    void shouldMapDouApiItemToPublicContestPostingEntityCoreFields() {
        DouContestNormalizer normalizer = new DouContestNormalizer();
        DouApiItemResponse response = new DouApiItemResponse(
                "dou-1",
                "Analista de TI - Desenvolvimento de Sistemas",
                "Concurso federal para analista de tecnologia da informacao",
                "2026-03-10",
                "https://www.in.gov.br/web/dou/-/edital-1"
        );

        PublicContestPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getExternalId()).isEqualTo("dou-1");
        assertThat(posting.getCanonicalUrl()).isEqualTo("https://www.in.gov.br/web/dou/-/edital-1");
        assertThat(posting.getContestName()).isEqualTo("Analista de TI - Desenvolvimento de Sistemas");
        assertThat(posting.getPositionTitle()).isEqualTo("Analista de TI - Desenvolvimento de Sistemas");
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(posting.getEditalUrl()).isEqualTo("https://www.in.gov.br/web/dou/-/edital-1");
    }

    @Test
    @DisplayName("should force government level to FEDERAL and organizer to DOU")
    void shouldForceGovernmentLevelToFederalAndOrganizerToDou() {
        DouContestNormalizer normalizer = new DouContestNormalizer();
        DouApiItemResponse response = new DouApiItemResponse(
                "dou-2",
                "Desenvolvedor Backend Java",
                "Processo seletivo com foco em tecnologia da informacao",
                "2026-03-11",
                "https://www.in.gov.br/web/dou/-/edital-2"
        );

        PublicContestPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getGovernmentLevel()).isEqualTo(GovernmentLevel.FEDERAL);
        assertThat(posting.getOrganizer()).isEqualTo("DOU");
        assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.OPEN);
    }

    @Test
    @DisplayName("should preserve original payload as json for audit")
    void shouldPreserveOriginalPayloadAsJsonForAudit() {
        DouContestNormalizer normalizer = new DouContestNormalizer();
        DouApiItemResponse response = new DouApiItemResponse(
                "dou-audit",
                "Analista de TI",
                "Resumo de tecnologia da informacao",
                "2026-03-12",
                "https://www.in.gov.br/web/dou/-/edital-audit"
        );

        PublicContestPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getPayloadJson()).contains("\"id\":\"dou-audit\"");
        assertThat(posting.getPayloadJson()).contains("\"detailUrl\":\"https://www.in.gov.br/web/dou/-/edital-audit\"");
    }
}
