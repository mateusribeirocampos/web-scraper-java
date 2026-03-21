package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
import com.campos.webscraper.interfaces.dto.GreenhouseLocationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("GreenhouseJobNormalizer")
class GreenhouseJobNormalizerTest {

    @Test
    @DisplayName("should map Greenhouse job board response to JobPostingEntity core fields")
    void shouldMapGreenhouseJobBoardResponseToJobPostingEntityCoreFields() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                6120911003L,
                "Senior Java Engineer",
                "https://bitso.com/jobs/6120911003?gh_jid=6120911003",
                "Bitso",
                new GreenhouseLocationResponse("Latin America", "Remote"),
                "2024-09-13T11:35:49-04:00",
                "<p>Join Bitso to build reliable Java services for crypto and payments.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getExternalId()).isEqualTo("6120911003");
        assertThat(posting.getCanonicalUrl()).isEqualTo("https://bitso.com/jobs/6120911003?gh_jid=6120911003");
        assertThat(posting.getTitle()).isEqualTo("Senior Java Engineer");
        assertThat(posting.getCompany()).isEqualTo("Bitso");
        assertThat(posting.getLocation()).isEqualTo("Latin America");
        assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2024, 9, 13));
        assertThat(posting.getDescription()).contains("Java services");
    }

    @Test
    @DisplayName("should infer seniority remote and tech stack tags from title and location")
    void shouldInferSeniorityRemoteAndTechStackTagsFromTitleAndLocation() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                6120911003L,
                "Senior Java Engineer",
                "https://bitso.com/jobs/6120911003?gh_jid=6120911003",
                "Bitso",
                new GreenhouseLocationResponse("Latin America", "Remote"),
                "2024-09-13T11:35:49-04:00",
                "<p>Join Bitso to build reliable Java services for crypto and payments.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getSeniority()).isEqualTo(SeniorityLevel.SENIOR);
        assertThat(posting.isRemote()).isTrue();
        assertThat(posting.getTechStackTags()).isEqualTo("Java");
    }

    @Test
    @DisplayName("should preserve original Greenhouse payload as json for audit")
    void shouldPreserveOriginalGreenhousePayloadAsJsonForAudit() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                7655700003L,
                "Senior Security Operations (SecOps) Engineer",
                "https://bitso.com/jobs/7655700003?gh_jid=7655700003",
                "Bitso",
                new GreenhouseLocationResponse("Latin America", null),
                "2026-03-06T11:09:26-05:00",
                "<p>Security engineering role for cloud and incident response operations.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getPayloadJson()).contains("\"id\":7655700003");
        assertThat(posting.getPayloadJson()).contains("\"company_name\":\"Bitso\"");
    }

    @Test
    @DisplayName("should mark posting as remote when Greenhouse location country is remote")
    void shouldMarkPostingAsRemoteWhenGreenhouseLocationCountryIsRemote() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                7000000001L,
                "Platform Engineer",
                "https://bitso.com/jobs/7000000001?gh_jid=7000000001",
                "Bitso",
                new GreenhouseLocationResponse("Sao Paulo, Brazil", "Remote"),
                "2026-03-10T11:09:26-05:00",
                "<p>Platform engineering for distributed systems.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.isRemote()).isTrue();
    }

    @Test
    @DisplayName("should avoid false positive seniority from unrelated substrings")
    void shouldAvoidFalsePositiveSeniorityFromUnrelatedSubstrings() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();

        JobPostingEntity srePosting = normalizer.normalize(new GreenhouseJobBoardItemResponse(
                1L,
                "SRE Engineer",
                "https://bitso.com/jobs/1?gh_jid=1",
                "Bitso",
                new GreenhouseLocationResponse("Latin America", null),
                "2026-03-10T11:09:26-05:00",
                "<p>Site reliability engineering.</p>"
        ));

        JobPostingEntity middlewarePosting = normalizer.normalize(new GreenhouseJobBoardItemResponse(
                2L,
                "Middleware Engineer",
                "https://bitso.com/jobs/2?gh_jid=2",
                "Bitso",
                new GreenhouseLocationResponse("Latin America", null),
                "2026-03-10T11:09:26-05:00",
                "<p>Integration platform engineering.</p>"
        ));

        JobPostingEntity internalToolsPosting = normalizer.normalize(new GreenhouseJobBoardItemResponse(
                3L,
                "Internal Tools Engineer",
                "https://bitso.com/jobs/3?gh_jid=3",
                "Bitso",
                new GreenhouseLocationResponse("Latin America", null),
                "2026-03-10T11:09:26-05:00",
                "<p>Developer productivity.</p>"
        ));

        assertThat(srePosting.getSeniority()).isNull();
        assertThat(middlewarePosting.getSeniority()).isNull();
        assertThat(internalToolsPosting.getSeniority()).isNull();
    }

    @Test
    @DisplayName("should avoid tagging JavaScript roles as Java")
    void shouldAvoidTaggingJavaScriptRolesAsJava() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                4L,
                "Senior JavaScript Engineer",
                "https://bitso.com/jobs/4?gh_jid=4",
                "Bitso",
                new GreenhouseLocationResponse("Latin America", null),
                "2026-03-10T11:09:26-05:00",
                "<p>Build modern frontends with JavaScript and TypeScript.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getTechStackTags()).isEqualTo("TypeScript");
    }

    @Test
    @DisplayName("should avoid tagging business text as Go")
    void shouldAvoidTaggingBusinessTextAsGo() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                5L,
                "Sales Executive",
                "https://example.com/jobs/5?gh_jid=5",
                "Wellhub",
                new GreenhouseLocationResponse("Brazil", null),
                "2026-03-10T11:09:26-05:00",
                "<p>We go above and beyond to help customers go through the sales funnel.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getTechStackTags()).isNull();
    }

    @Test
    @DisplayName("should tag Go only when technical context is present")
    void shouldTagGoOnlyWhenTechnicalContextIsPresent() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                6L,
                "Backend Engineer",
                "https://example.com/jobs/6?gh_jid=6",
                "Wellhub",
                new GreenhouseLocationResponse("Brazil", null),
                "2026-03-10T11:09:26-05:00",
                "<p>Build backend services in Go and PostgreSQL for payments.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getTechStackTags()).isEqualTo("Go,PostgreSQL");
    }

    @Test
    @DisplayName("should avoid tagging Python for non technical ops roles")
    void shouldAvoidTaggingPythonForNonTechnicalOpsRoles() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                7L,
                "AML Ops Analyst",
                "https://example.com/jobs/7?gh_jid=7",
                "Nubank",
                new GreenhouseLocationResponse("Sao Paulo", null),
                "2026-03-10T11:09:26-05:00",
                "<ul><li>Have knowledge in Excel, Sheets, Looker, SQL and Python.</li></ul>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getTechStackTags()).isNull();
    }

    @Test
    @DisplayName("should tag Python for clearly technical engineering roles")
    void shouldTagPythonForClearlyTechnicalEngineeringRoles() {
        GreenhouseJobNormalizer normalizer = new GreenhouseJobNormalizer();
        GreenhouseJobBoardItemResponse response = new GreenhouseJobBoardItemResponse(
                8L,
                "Staff Platform Engineer | Observability",
                "https://example.com/jobs/8?gh_jid=8",
                "Wellhub",
                new GreenhouseLocationResponse("Brazil", null),
                "2026-03-10T11:09:26-05:00",
                "<p>You will also be able to code in Golang, Ruby, or Python to build and maintain our products and tools.</p>"
        );

        JobPostingEntity posting = normalizer.normalize(response);

        assertThat(posting.getTechStackTags()).isEqualTo("Python,Go");
    }
}
