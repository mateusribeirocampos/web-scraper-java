package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CrawlExecutionEntity — validates field mapping and builder behaviour.
 *
 * TDD RED: written before the entity exists.
 */
@DisplayName("CrawlExecutionEntity")
class CrawlExecutionEntityTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build a minimal CrawlExecutionEntity with required fields")
        void shouldBuildMinimalEntity() {
            Instant now = Instant.now();
            CrawlJobEntity job = CrawlJobEntity.builder()
                    .scheduledAt(now)
                    .createdAt(now)
                    .build();

            CrawlExecutionEntity execution = CrawlExecutionEntity.builder()
                    .crawlJob(job)
                    .status(CrawlExecutionStatus.PENDING)
                    .createdAt(now)
                    .build();

            assertThat(execution.getCrawlJob()).isEqualTo(job);
            assertThat(execution.getStatus()).isEqualTo(CrawlExecutionStatus.PENDING);
            assertThat(execution.getCreatedAt()).isEqualTo(now);
            assertThat(execution.getId()).isNull();
        }

        @Test
        @DisplayName("should build CrawlExecutionEntity with lifecycle fields")
        void shouldBuildWithLifecycleFields() {
            Instant now = Instant.now();
            Instant later = now.plusSeconds(60);

            CrawlJobEntity job = CrawlJobEntity.builder()
                    .scheduledAt(now)
                    .createdAt(now)
                    .build();

            CrawlExecutionEntity execution = CrawlExecutionEntity.builder()
                    .crawlJob(job)
                    .status(CrawlExecutionStatus.SUCCEEDED)
                    .startedAt(now)
                    .finishedAt(later)
                    .pagesVisited(5)
                    .itemsFound(42)
                    .retryCount(0)
                    .createdAt(now)
                    .build();

            assertThat(execution.getStatus()).isEqualTo(CrawlExecutionStatus.SUCCEEDED);
            assertThat(execution.getStartedAt()).isEqualTo(now);
            assertThat(execution.getFinishedAt()).isEqualTo(later);
            assertThat(execution.getPagesVisited()).isEqualTo(5);
            assertThat(execution.getItemsFound()).isEqualTo(42);
            assertThat(execution.getRetryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should build CrawlExecutionEntity with error message for FAILED status")
        void shouldBuildWithErrorMessage() {
            Instant now = Instant.now();
            CrawlJobEntity job = CrawlJobEntity.builder()
                    .scheduledAt(now)
                    .createdAt(now)
                    .build();

            CrawlExecutionEntity execution = CrawlExecutionEntity.builder()
                    .crawlJob(job)
                    .status(CrawlExecutionStatus.FAILED)
                    .retryCount(2)
                    .errorMessage("Connection timeout after 30s")
                    .createdAt(now)
                    .build();

            assertThat(execution.getStatus()).isEqualTo(CrawlExecutionStatus.FAILED);
            assertThat(execution.getRetryCount()).isEqualTo(2);
            assertThat(execution.getErrorMessage()).isEqualTo("Connection timeout after 30s");
        }
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTests {

        @Test
        @DisplayName("all CrawlExecutionStatus values should be assignable")
        void allStatusValuesShouldBeAssignable() {
            Instant now = Instant.now();
            CrawlJobEntity job = CrawlJobEntity.builder()
                    .scheduledAt(now)
                    .createdAt(now)
                    .build();

            for (CrawlExecutionStatus status : CrawlExecutionStatus.values()) {
                CrawlExecutionEntity execution = CrawlExecutionEntity.builder()
                        .crawlJob(job)
                        .status(status)
                        .createdAt(now)
                        .build();
                assertThat(execution.getStatus()).isEqualTo(status);
            }
        }
    }
}
