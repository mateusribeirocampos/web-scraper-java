package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("OnboardingOperationalCheckResult — activation readiness signal")
class OnboardingOperationalCheckResultTest {

    @Nested
    @DisplayName("activationReady()")
    class ActivationReady {

        @Test
        @DisplayName("returns true when execution succeeded and postings were found")
        void returnsTrueWhenExecutionSucceededAndPostingsWereFound() {
            OnboardingOperationalCheckResult result = resultWith(succeededExecution(), 3, null);

            assertThat(result.activationReady()).isTrue();
        }

        @Test
        @DisplayName("returns false when execution succeeded but no postings were found")
        void returnsFalseWhenExecutionSucceededButNoPostingsWereFound() {
            OnboardingOperationalCheckResult result = resultWith(succeededExecution(), 0, null);

            assertThat(result.activationReady()).isFalse();
        }

        @Test
        @DisplayName("returns false when execution failed")
        void returnsFalseWhenExecutionFailed() {
            OnboardingOperationalCheckResult result = resultWith(failedExecution(), 3, null);

            assertThat(result.activationReady()).isFalse();
        }

        @Test
        @DisplayName("returns false when there is no execution summary")
        void returnsFalseWhenThereIsNoExecutionSummary() {
            OnboardingOperationalCheckResult result = resultWith(null, 0, null);

            assertThat(result.activationReady()).isFalse();
        }

        @Test
        @DisplayName("returns false when smoke run was blocked by compliance")
        void returnsFalseWhenSmokeRunWasBlockedByCompliance() {
            TargetSiteSmokeRunResult blockedSmokeRun = new TargetSiteSmokeRunResult(
                    1L, "lever_watchguard", 10L, BootstrapStatus.UPDATED,
                    "BLOCKED_BY_COMPLIANCE", null
            );
            OnboardingOperationalCheckResult result = resultWith(succeededExecution(), 3, blockedSmokeRun);

            assertThat(result.activationReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("activationBlockers()")
    class ActivationBlockers {

        @Test
        @DisplayName("returns empty list when activation is ready")
        void returnsEmptyListWhenActivationIsReady() {
            OnboardingOperationalCheckResult result = resultWith(succeededExecution(), 3, null);

            assertThat(result.activationBlockers()).isEmpty();
        }

        @Test
        @DisplayName("reports missing execution when no summary present")
        void reportsMissingExecutionWhenNoSummaryPresent() {
            OnboardingOperationalCheckResult result = resultWith(null, 0, null);

            assertThat(result.activationBlockers())
                    .contains("no execution recorded for this source");
        }

        @Test
        @DisplayName("reports failed execution status")
        void reportsFailedExecutionStatus() {
            OnboardingOperationalCheckResult result = resultWith(failedExecution(), 0, null);

            assertThat(result.activationBlockers())
                    .contains("last execution did not succeed (status: FAILED)");
        }

        @Test
        @DisplayName("reports zero postings when execution succeeded but nothing was collected")
        void reportsZeroPostingsWhenExecutionSucceededButNothingWasCollected() {
            OnboardingOperationalCheckResult result = resultWith(succeededExecution(), 0, null);

            assertThat(result.activationBlockers())
                    .contains("no postings collected in the observed window");
        }

        @Test
        @DisplayName("reports compliance block when smoke run was blocked")
        void reportsComplianceBlockWhenSmokeRunWasBlocked() {
            TargetSiteSmokeRunResult blockedSmokeRun = new TargetSiteSmokeRunResult(
                    1L, "lever_watchguard", 10L, BootstrapStatus.UPDATED,
                    "BLOCKED_BY_COMPLIANCE", null
            );
            OnboardingOperationalCheckResult result = resultWith(succeededExecution(), 3, blockedSmokeRun);

            assertThat(result.activationBlockers())
                    .contains("smoke run was blocked by compliance — legal review still required");
        }

        @Test
        @DisplayName("reports multiple blockers when several conditions fail")
        void reportsMultipleBlockersWhenSeveralConditionsFail() {
            OnboardingOperationalCheckResult result = resultWith(failedExecution(), 0, null);

            assertThat(result.activationBlockers()).hasSize(2);
            assertThat(result.activationBlockers())
                    .contains("last execution did not succeed (status: FAILED)")
                    .contains("no postings collected in the observed window");
        }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static OnboardingOperationalCheckResult resultWith(
            OnboardingOperationalCheckExecutionSummary executionSummary,
            int recentPostingsCount,
            TargetSiteSmokeRunResult smokeRun
    ) {
        TargetSiteEntity site = TargetSiteEntity.builder()
                .id(1L)
                .siteCode("test_site")
                .displayName("Test Site")
                .baseUrl("https://example.org")
                .build();

        CrawlJobEntity job = CrawlJobEntity.builder()
                .id(10L)
                .targetSite(site)
                .createdAt(Instant.parse("2026-04-12T10:00:00Z"))
                .build();

        BootstrappedOnboardingWorkflowResult workflow = new BootstrappedOnboardingWorkflowResult(
                "test_profile",
                new BootstrappedTargetSite("test_profile", BootstrapStatus.UPDATED, site),
                new BootstrappedCrawlJob(BootstrapStatus.UPDATED, job),
                smokeRun != null,
                smokeRun
        );

        return new OnboardingOperationalCheckResult(
                "test_profile",
                workflow,
                executionSummary,
                recentPostingsCount,
                List.of()
        );
    }

    private static OnboardingOperationalCheckExecutionSummary succeededExecution() {
        return new OnboardingOperationalCheckExecutionSummary(
                10L, 100L, "SUCCEEDED", 5,
                Instant.parse("2026-04-12T10:00:00Z"),
                Instant.parse("2026-04-12T10:00:10Z")
        );
    }

    private static OnboardingOperationalCheckExecutionSummary failedExecution() {
        return new OnboardingOperationalCheckExecutionSummary(
                10L, 100L, "FAILED", 0,
                Instant.parse("2026-04-12T10:00:00Z"),
                Instant.parse("2026-04-12T10:00:10Z")
        );
    }
}
