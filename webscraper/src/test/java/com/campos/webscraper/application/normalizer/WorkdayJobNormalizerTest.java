package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.WorkdayJobPostingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("WorkdayJobNormalizer")
class WorkdayJobNormalizerTest {

    @Test
    @DisplayName("should map Airbus Helibras Workday posting into canonical private-sector entity")
    void shouldMapAirbusHelibrasWorkdayPostingIntoCanonicalPrivateSectorEntity() {
        WorkdayJobNormalizer normalizer = new WorkdayJobNormalizer(
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-04-08T12:00:00Z"), ZoneOffset.UTC)
        );

        JobPostingEntity posting = normalizer.normalize(
                "airbus_helibras_workday",
                new WorkdayJobPostingResponse(
                        "Estágio Técnico em Produção",
                        "/job/Itajub/Estgio-Tcnico-em-Produo_JR10397592",
                        "Itajubá",
                        "Posted Yesterday",
                        List.of("JR10397592")
                )
        );

        assertThat(posting.getExternalId()).isEqualTo("JR10397592");
        assertThat(posting.getCanonicalUrl())
                .isEqualTo("https://ag.wd3.myworkdayjobs.com/en-US/Airbus/job/Itajub/Estgio-Tcnico-em-Produo_JR10397592");
        assertThat(posting.getCompany()).isEqualTo("Helibras / Airbus");
        assertThat(posting.getLocation()).isEqualTo("Itajubá");
        assertThat(posting.getContractType()).isEqualTo(JobContractType.INTERNSHIP);
        assertThat(posting.getSeniority()).isEqualTo(SeniorityLevel.INTERN);
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2026, 4, 7));
        assertThat(posting.getPayloadJson()).contains("JR10397592");
    }

    @Test
    @DisplayName("should parse relative postedOn values and enrich Workday-specific tags")
    void shouldParseRelativePostedOnValuesAndEnrichWorkdaySpecificTags() {
        WorkdayJobNormalizer normalizer = new WorkdayJobNormalizer(
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-04-08T12:00:00Z"), ZoneOffset.UTC)
        );

        JobPostingEntity posting = normalizer.normalize(
                "airbus_helibras_workday",
                new WorkdayJobPostingResponse(
                        "Analista de Sistemas SAP - SR | Senior SAP System Analyst",
                        "/job/Itajub/Analista-de-Sistemas-SAP---SR---Senior-SAP-System-Analyst_JR10374076",
                        "2 Locations",
                        "Posted 30+ Days Ago",
                        List.of("JR10374076")
                )
        );

        assertThat(posting.getContractType()).isEqualTo(JobContractType.UNKNOWN);
        assertThat(posting.getSeniority()).isEqualTo(SeniorityLevel.SENIOR);
        assertThat(posting.getTechStackTags()).isEqualTo("SAP");
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 9));
    }
}
