package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.JobCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CrawlJobEntity — validates field mapping and builder behaviour.
 *
 * TDD RED: written before the entity exists.
 */
@DisplayName("CrawlJobEntity")
class CrawlJobEntityTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build a minimal CrawlJobEntity with required fields")
        void shouldBuildMinimalEntity() {
            Instant now = Instant.now();

            CrawlJobEntity job = CrawlJobEntity.builder()
                    .scheduledAt(now)
                    .createdAt(now)
                    .build();

            assertThat(job.getScheduledAt()).isEqualTo(now);
            assertThat(job.getCreatedAt()).isEqualTo(now);
            assertThat(job.getId()).isNull();
            assertThat(job.getTargetSite()).isNull();
        }

        @Test
        @DisplayName("should build CrawlJobEntity with all fields set")
        void shouldBuildFullEntity() {
            Instant now = Instant.now();
            TargetSiteEntity site = TargetSiteEntity.builder()
                    .siteCode("test-site")
                    .displayName("Test Site")
                    .baseUrl("https://test.com")
                    .enabled(true)
                    .createdAt(now)
                    .build();

            CrawlJobEntity job = CrawlJobEntity.builder()
                    .targetSite(site)
                    .scheduledAt(now)
                    .jobCategory(JobCategory.PRIVATE_SECTOR)
                    .createdAt(now)
                    .build();

            assertThat(job.getTargetSite()).isEqualTo(site);
            assertThat(job.getScheduledAt()).isEqualTo(now);
            assertThat(job.getJobCategory()).isEqualTo(JobCategory.PRIVATE_SECTOR);
            assertThat(job.getCreatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Defaults")
    class DefaultTests {

        @Test
        @DisplayName("jobCategory should be nullable (optional override)")
        void jobCategoryShouldBeNullable() {
            CrawlJobEntity job = CrawlJobEntity.builder()
                    .scheduledAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            assertThat(job.getJobCategory()).isNull();
        }
    }
}
