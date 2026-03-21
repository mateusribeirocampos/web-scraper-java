package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.JobPostingSearchProfile;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("JobPostingSearchProfileMatcher")
class JobPostingSearchProfileMatcherTest {

    private final JobPostingSearchProfileMatcher matcher = new JobPostingSearchProfileMatcher();

    @Test
    @DisplayName("should accept junior java backend postings for the default profile")
    void shouldAcceptJuniorJavaBackendPostingsForTheDefaultProfile() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Desenvolvedor Backend Java Jr")
                .company("Acme")
                .seniority(SeniorityLevel.JUNIOR)
                .techStackTags("Java,Spring")
                .description("Backend Java com Spring Boot")
                .canonicalUrl("https://example.com/jobs/1")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_JUNIOR_BACKEND)).isTrue();
    }

    @Test
    @DisplayName("should reject talent pool postings for the default profile")
    void shouldRejectTalentPoolPostingsForTheDefaultProfile() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Banco de Talentos - Desenvolvedor Java")
                .company("Acme")
                .seniority(SeniorityLevel.JUNIOR)
                .techStackTags("Java")
                .canonicalUrl("https://example.com/jobs/2")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_JUNIOR_BACKEND)).isFalse();
    }

    @Test
    @DisplayName("should reject senior postings for the default profile")
    void shouldRejectSeniorPostingsForTheDefaultProfile() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Senior Software Engineer")
                .company("Acme")
                .seniority(SeniorityLevel.SENIOR)
                .techStackTags("Java,Spring")
                .canonicalUrl("https://example.com/jobs/3")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_JUNIOR_BACKEND)).isFalse();
    }

    @Test
    @DisplayName("should reject generic backend titles without java stack signal")
    void shouldRejectGenericBackendTitlesWithoutJavaStackSignal() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Backend Software Engineer")
                .company("Acme")
                .seniority(SeniorityLevel.MID)
                .description("Trabalho com alta escala e produtos digitais")
                .canonicalUrl("https://example.com/jobs/4")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_JUNIOR_BACKEND)).isFalse();
    }

    @Test
    @DisplayName("should reject leadership roles even with java stack signal")
    void shouldRejectLeadershipRolesEvenWithJavaStackSignal() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Engineering Manager")
                .company("Acme")
                .techStackTags("Java,Spring")
                .description("Engineering leadership for Java platform")
                .canonicalUrl("https://example.com/jobs/6")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_JUNIOR_BACKEND)).isFalse();
    }

    @Test
    @DisplayName("should reject postings without a compatible role signal")
    void shouldRejectPostingsWithoutACompatibleRoleSignal() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Platform Specialist")
                .company("Acme")
                .techStackTags("Java,Spring")
                .description("Atuacao em plataforma interna")
                .canonicalUrl("https://example.com/jobs/7")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_JUNIOR_BACKEND)).isFalse();
    }

    @Test
    @DisplayName("should accept senior java postings for the balanced profile")
    void shouldAcceptSeniorJavaPostingsForTheBalancedProfile() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Senior Software Engineer")
                .company("Acme")
                .seniority(SeniorityLevel.SENIOR)
                .techStackTags("Java,Spring")
                .description("Java platform engineering")
                .canonicalUrl("https://example.com/jobs/8")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_BACKEND_BALANCED)).isTrue();
    }

    @Test
    @DisplayName("should accept java postings without strict role signal for the balanced profile")
    void shouldAcceptJavaPostingsWithoutStrictRoleSignalForTheBalancedProfile() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Platform Specialist")
                .company("Acme")
                .techStackTags("Java,Spring")
                .description("Atuacao em plataforma interna")
                .canonicalUrl("https://example.com/jobs/9")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_BACKEND_BALANCED)).isTrue();
    }

    @Test
    @DisplayName("should still reject leadership roles for the balanced profile")
    void shouldStillRejectLeadershipRolesForTheBalancedProfile() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Engineering Manager")
                .company("Acme")
                .techStackTags("Java,Spring")
                .description("Engineering leadership for Java platform")
                .canonicalUrl("https://example.com/jobs/10")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.JAVA_BACKEND_BALANCED)).isFalse();
    }

    @Test
    @DisplayName("should allow unfiltered profile")
    void shouldAllowUnfilteredProfile() {
        JobPostingEntity posting = JobPostingEntity.builder()
                .title("Senior Software Engineer")
                .company("Acme")
                .seniority(SeniorityLevel.SENIOR)
                .canonicalUrl("https://example.com/jobs/11")
                .build();

        assertThat(matcher.matches(posting, JobPostingSearchProfile.UNFILTERED)).isTrue();
    }
}
