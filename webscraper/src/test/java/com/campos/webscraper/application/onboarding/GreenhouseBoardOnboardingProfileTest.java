package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("GreenhouseBoardOnboardingProfile")
class GreenhouseBoardOnboardingProfileTest {

    private final TargetSiteOnboardingValidator validator = new TargetSiteOnboardingValidator();

    @Test
    @DisplayName("should expose the selected Bitso Greenhouse board as an API-backed private-sector target site")
    void shouldExposeTheSelectedBitsoGreenhouseBoardAsAnApiBackedPrivateSectorTargetSite() {
        GreenhouseBoardOnboardingProfile profile = GreenhouseBoardOnboardingProfiles.bitso();

        assertThat(profile.boardToken()).isEqualTo("bitso");
        assertThat(profile.jobsApiUrl()).isEqualTo("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true");
        assertThat(profile.targetSite().getSiteCode()).isEqualTo("greenhouse_bitso");
        assertThat(profile.targetSite().getSiteType()).isEqualTo(SiteType.TYPE_E);
        assertThat(profile.targetSite().getExtractionMode()).isEqualTo(ExtractionMode.API);
        assertThat(profile.targetSite().getJobCategory()).isEqualTo(JobCategory.PRIVATE_SECTOR);
        assertThat(profile.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(profile.targetSite().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should keep the selected Bitso Greenhouse board pending until a scraper strategy exists")
    void shouldKeepTheSelectedBitsoGreenhouseBoardPendingUntilAScraperStrategyExists() {
        GreenhouseBoardOnboardingProfile profile = GreenhouseBoardOnboardingProfiles.bitso();

        TargetSiteOnboardingDecision decision = validator.assess(profile.targetSite(), profile.checklist());

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("scraper strategy not implemented for target site");
    }
}
