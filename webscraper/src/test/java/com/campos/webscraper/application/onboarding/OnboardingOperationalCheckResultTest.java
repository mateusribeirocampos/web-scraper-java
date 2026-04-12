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
        @DisplayName("returns true when execution succeeded with postings found in the requested window")
        void returnsTrueWhenExecutionSucceededWithPostingsInRequestedWindow() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 5, true, dispatchedSmokeRun());

            assertThat(result.activationReady()).isTrue();
        }

        @Test
        @DisplayName("returns true for observation-only call when previous execution has recent postings")
        void returnsTrueForObservationOnlyCallWhenPreviousExecutionHasRecentPostings() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 5, false, null);

            assertThat(result.activationReady()).isTrue();
        }

        @Test
        @DisplayName("returns false when smoke run was blocked by compliance")
        void returnsFalseWhenSmokeRunWasBlockedByCompliance() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 3, true, blockedSmokeRun());

            assertThat(result.activationReady()).isFalse();
        }

        @Test
        @DisplayName("returns false when execution failed")
        void returnsFalseWhenExecutionFailed() {
            OnboardingOperationalCheckResult result =
                    resultWith(failedExecution(), 3, true, dispatchedSmokeRun());

            assertThat(result.activationReady()).isFalse();
        }

        @Test
        @DisplayName("returns false when there is no execution summary")
        void returnsFalseWhenThereIsNoExecutionSummary() {
            OnboardingOperationalCheckResult result =
                    resultWith(null, 0, true, dispatchedSmokeRun());

            assertThat(result.activationReady()).isFalse();
        }

        @Test
        @DisplayName("returns false when execution succeeded but no postings in the requested window")
        void returnsFalseWhenExecutionSucceededButNoPostingsInRequestedWindow() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 0, true, dispatchedSmokeRun());

            assertThat(result.activationReady()).isFalse();
        }

        @Test
        @DisplayName("returns false when source only imports historical items outside the requested window")
        void returnsFalseWhenOnlyHistoricalItemsImportedOutsideWindow() {
            // succeededExecution() has itemsFound=5 (historical), but recentPostingsCount=0
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 0, true, dispatchedSmokeRun());

            assertThat(result.activationReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("activationBlockers()")
    class ActivationBlockers {

        @Test
        @DisplayName("returns empty list when execution succeeded with recent postings in window")
        void returnsEmptyListWhenActivationIsReady() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 5, true, dispatchedSmokeRun());

            assertThat(result.activationBlockers()).isEmpty();
        }

        @Test
        @DisplayName("does not block observation-only calls when previous execution has recent postings")
        void doesNotBlockObservationOnlyCallsWhenPreviousExecutionHasRecentPostings() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 5, false, null);

            assertThat(result.activationBlockers()).isEmpty();
        }

        @Test
        @DisplayName("reports compliance block when smoke run was blocked")
        void reportsComplianceBlockWhenSmokeRunWasBlocked() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 3, true, blockedSmokeRun());

            assertThat(result.activationBlockers())
                    .containsExactly(
                            "smoke run was blocked by compliance — legal review still required"
                    );
        }

        @Test
        @DisplayName("reports missing execution when no summary is present")
        void reportsMissingExecutionWhenNoSummaryPresent() {
            OnboardingOperationalCheckResult result =
                    resultWith(null, 0, true, dispatchedSmokeRun());

            assertThat(result.activationBlockers())
                    .containsExactly("no execution recorded for this source");
        }

        @Test
        @DisplayName("reports failed execution status")
        void reportsFailedExecutionStatus() {
            OnboardingOperationalCheckResult result =
                    resultWith(failedExecution(), 0, true, dispatchedSmokeRun());

            assertThat(result.activationBlockers())
                    .containsExactly("last execution did not succeed (status: FAILED)");
        }

        @Test
        @DisplayName("reports no postings in window when execution succeeded but window is empty")
        void reportsNoPostingsInWindowWhenWindowIsEmpty() {
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 0, true, dispatchedSmokeRun());

            assertThat(result.activationBlockers())
                    .containsExactly("execution succeeded but no postings found in the requested window — source may only contain historical items");
        }

        @Test
        @DisplayName("reports window blocker even when source imported historical items outside window")
        void reportsWindowBlockerEvenWhenSourceHasHistoricalItems() {
            // succeededExecution() has itemsFound=5 (historical), but recentPostingsCount=0
            OnboardingOperationalCheckResult result =
                    resultWith(succeededExecution(), 0, true, dispatchedSmokeRun());

            assertThat(result.activationBlockers())
                    .containsExactly("execution succeeded but no postings found in the requested window — source may only contain historical items");
        }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static OnboardingOperationalCheckResult resultWith(
            OnboardingOperationalCheckExecutionSummary executionSummary,
            int recentPostingsCount,
            boolean smokeRunRequested,
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
                smokeRunRequested,
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

    private static TargetSiteSmokeRunResult dispatchedSmokeRun() {
        return new TargetSiteSmokeRunResult(
                1L, "test_site", 10L, BootstrapStatus.UPDATED,
                "DISPATCHED", CrawlExecutionStatus.SUCCEEDED
        );
    }

    private static TargetSiteSmokeRunResult blockedSmokeRun() {
        return new TargetSiteSmokeRunResult(
                1L, "lever_watchguard", 10L, BootstrapStatus.UPDATED,
                "BLOCKED_BY_COMPLIANCE", null
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
