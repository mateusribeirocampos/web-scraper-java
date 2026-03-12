package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.IndeedApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IndeedJobNormalizer.
 *
 * TDD RED: written before the normalizer exists.
 */
@Tag("unit")
@DisplayName("IndeedJobNormalizer")
class IndeedJobNormalizerTest {

    @Test
    @DisplayName("should map Indeed API response to JobPostingEntity core fields")
    void shouldMapIndeedApiResponseToJobPostingEntityCoreFields() {
        IndeedJobNormalizer normalizer = new IndeedJobNormalizer();
        IndeedApiResponse response = new IndeedApiResponse(
                "5-cmh1-0-1jj9snbbvr8er800-358c3bd3a6b73ba5",
                "Java Backend Developer | Jr (Remote)",
                "Invillia",
                "Remoto",
                "2026-03-05",
                "https://to.indeed.com/aas2tpyk2v6d"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getExternalId()).isEqualTo("5-cmh1-0-1jj9snbbvr8er800-358c3bd3a6b73ba5");
        assertThat(posting.getCanonicalUrl()).isEqualTo("https://to.indeed.com/aas2tpyk2v6d");
        assertThat(posting.getTitle()).isEqualTo("Java Backend Developer | Jr (Remote)");
        assertThat(posting.getCompany()).isEqualTo("Invillia");
        assertThat(posting.getLocation()).isEqualTo("Remoto");
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    @DisplayName("should force seniority to JUNIOR and tech stack tags to Java Spring Boot")
    void shouldForceSeniorityToJuniorAndTechStackTagsToJavaSpringBoot() {
        IndeedJobNormalizer normalizer = new IndeedJobNormalizer();
        IndeedApiResponse response = new IndeedApiResponse(
                "job-123",
                "Java Developer",
                "Invillia",
                "Sao Paulo, SP",
                "2026-03-06",
                "https://to.indeed.com/job-123"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getSeniority()).isEqualTo(SeniorityLevel.JUNIOR);
        assertThat(posting.getTechStackTags()).isEqualTo("Java,Spring Boot");
    }

    @Test
    @DisplayName("should mark posting as remote when title or location indicates remote work")
    void shouldMarkPostingAsRemoteWhenTitleOrLocationIndicatesRemoteWork() {
        IndeedJobNormalizer normalizer = new IndeedJobNormalizer();
        IndeedApiResponse response = new IndeedApiResponse(
                "job-remote",
                "Java Backend Developer | Jr (Remote)",
                "Invillia",
                "Remoto",
                "2026-03-05",
                "https://to.indeed.com/job-remote"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.isRemote()).isTrue();
    }

    @Test
    @DisplayName("should preserve original payload as json for audit")
    void shouldPreserveOriginalPayloadAsJsonForAudit() {
        IndeedJobNormalizer normalizer = new IndeedJobNormalizer();
        IndeedApiResponse response = new IndeedApiResponse(
                "job-audit",
                "Java Developer",
                "Invillia",
                "Remoto",
                "2026-03-05",
                "https://to.indeed.com/job-audit"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getPayloadJson()).contains("\"jobId\":\"job-audit\"");
        assertThat(posting.getPayloadJson()).contains("\"company\":\"Invillia\"");
    }
}
