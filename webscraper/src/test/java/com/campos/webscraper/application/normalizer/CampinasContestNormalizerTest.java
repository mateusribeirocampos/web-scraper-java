package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.CampinasContestPreviewItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CampinasContestNormalizer")
class CampinasContestNormalizerTest {

    @Test
    @DisplayName("should fall back to contest title when position title cannot be inferred")
    void shouldFallBackToContestTitleWhenPositionTitleCannotBeInferred() {
        CampinasContestNormalizer normalizer = new CampinasContestNormalizer(new ObjectMapper());

        CampinasContestPreviewItem item = new CampinasContestPreviewItem(
                "Inscrições abertas do concurso público 2026",
                "Prefeitura Municipal de Campinas",
                null,
                "PCAM2602",
                "https://campinas.sp.gov.br/sites/concursos/",
                "https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658",
                "https://www.vunesp.com.br/PCAM2602",
                LocalDate.parse("2026-03-19"),
                null,
                null
        );

        PublicContestPostingEntity posting = normalizer.normalize(
                item,
                LocalDateTime.parse("2026-03-31T10:15:00")
        );

        assertThat(posting.getPositionTitle()).isEqualTo("Inscrições abertas do concurso público 2026");
        assertThat(posting.getRegistrationStartDate()).isNull();
        assertThat(posting.getRegistrationEndDate()).isNull();
        assertThat(posting.getPayloadJson()).contains("\"positionTitle\":\"\"");
    }
}
