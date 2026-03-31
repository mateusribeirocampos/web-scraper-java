package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.LeverCategoriesResponse;
import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("LeverJobNormalizer")
class LeverJobNormalizerTest {

    private final LeverJobNormalizer normalizer = new LeverJobNormalizer(
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-03-31T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    @DisplayName("should map Lever posting response to JobPostingEntity core fields")
    void shouldMapLeverPostingResponseToJobPostingEntityCoreFields() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-123",
                "Senior Java Engineer",
                "https://jobs.lever.co/ciandt/job-123",
                "https://jobs.lever.co/ciandt/job-123/apply",
                "onsite",
                new LeverCategoriesResponse("Engineering", "Campinas, Brazil", "Full-time"),
                "<p>Build Java services on AWS.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getExternalId()).isEqualTo("job-123");
        assertThat(posting.getCanonicalUrl()).isEqualTo("https://jobs.lever.co/ciandt/job-123");
        assertThat(posting.getTitle()).isEqualTo("Senior Java Engineer");
        assertThat(posting.getCompany()).isEqualTo("CI&T");
        assertThat(posting.getLocation()).isEqualTo("Campinas, Brazil");
        assertThat(posting.getPublishedAt()).isNull();
        assertThat(posting.getDescription()).contains("Java services");
        assertThat(posting.getContractType()).isEqualTo(JobContractType.UNKNOWN);
    }

    @Test
    @DisplayName("should infer seniority remote and contract type from Lever fields")
    void shouldInferSeniorityRemoteAndContractTypeFromLeverFields() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-124",
                "Senior Platform Engineer Remote",
                "https://jobs.lever.co/ciandt/job-124",
                "https://jobs.lever.co/ciandt/job-124/apply",
                "remote",
                new LeverCategoriesResponse("Engineering", "Remote - Brazil", "Internship"),
                "<p>Platform engineering role.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getSeniority()).isEqualTo(SeniorityLevel.SENIOR);
        assertThat(posting.isRemote()).isTrue();
        assertThat(posting.getContractType()).isEqualTo(JobContractType.INTERNSHIP);
    }

    @Test
    @DisplayName("should leave publishedAt null when Lever payload has no stable source date")
    void shouldLeavePublishedAtNullWhenLeverPayloadHasNoStableSourceDate() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-126",
                "Backend Engineer",
                "https://jobs.lever.co/ciandt/job-126",
                "https://jobs.lever.co/ciandt/job-126/apply",
                "onsite",
                new LeverCategoriesResponse("Engineering", "Brazil", "Full-time"),
                "<p>Java backend role.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("should derive company from applyUrl when hostedUrl is missing")
    void shouldDeriveCompanyFromApplyUrlWhenHostedUrlIsMissing() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-127",
                "Backend Engineer",
                null,
                "https://jobs.lever.co/ciandt/job-127/apply",
                "onsite",
                new LeverCategoriesResponse("Engineering", "Brazil", "Full-time"),
                "<p>Java backend role.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getCanonicalUrl()).isEqualTo("https://jobs.lever.co/ciandt/job-127");
        assertThat(posting.getCompany()).isEqualTo("CI&T");
    }

    @Test
    @DisplayName("should derive generic company name from Lever board slug")
    void shouldDeriveGenericCompanyNameFromLeverBoardSlug() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-128",
                "Platform Engineer",
                "https://jobs.lever.co/acme-tech/job-128",
                "https://jobs.lever.co/acme-tech/job-128/apply",
                "onsite",
                new LeverCategoriesResponse("Engineering", "Brazil", "Full-time"),
                "<p>Platform role.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getCompany()).isEqualTo("Acme Tech");
    }

    @Test
    @DisplayName("should derive company from applyUrl when hostedUrl is branded")
    void shouldDeriveCompanyFromApplyUrlWhenHostedUrlIsBranded() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-130",
                "Platform Engineer",
                "https://jobs.example.com/platform-engineer",
                "https://jobs.lever.co/acme-tech/job-130/apply",
                "onsite",
                new LeverCategoriesResponse("Engineering", "Brazil", "Full-time"),
                "<p>Platform role.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getCanonicalUrl()).isEqualTo("https://jobs.example.com/platform-engineer");
        assertThat(posting.getCompany()).isEqualTo("Acme Tech");
    }

    @Test
    @DisplayName("should preserve original Lever payload as json for audit")
    void shouldPreserveOriginalLeverPayloadAsJsonForAudit() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-125",
                "Fullstack Developer",
                "https://jobs.lever.co/ciandt/job-125",
                "https://jobs.lever.co/ciandt/job-125/apply",
                "hybrid",
                new LeverCategoriesResponse("Engineering", "Brazil", "Full-time"),
                "<p>Node.js and React role.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getPayloadJson()).contains("\"id\":\"job-125\"");
        assertThat(posting.getPayloadJson()).contains("\"hostedUrl\":\"https://jobs.lever.co/ciandt/job-125\"");
    }

    @Test
    @DisplayName("should mark posting as remote when Lever workplaceType is remote")
    void shouldMarkPostingAsRemoteWhenLeverWorkplaceTypeIsRemote() {
        LeverPostingResponse response = new LeverPostingResponse(
                "job-129",
                "Data Engineer",
                "https://jobs.lever.co/ciandt/job-129",
                "https://jobs.lever.co/ciandt/job-129/apply",
                "remote",
                new LeverCategoriesResponse("Engineering", "Brazil", "Full-time"),
                "<p>Data platform role.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.isRemote()).isTrue();
    }
}
