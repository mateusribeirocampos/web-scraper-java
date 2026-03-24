package com.campos.webscraper.application.onboarding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("TargetSiteOnboardingProfileCatalog")
class TargetSiteOnboardingProfileCatalogTest {

    private final TargetSiteOnboardingProfileCatalog catalog = new TargetSiteOnboardingProfileCatalog();

    @Test
    @DisplayName("should expose curated onboarding profiles for supported sources")
    void shouldExposeCuratedOnboardingProfilesForSupportedSources() {
        assertThat(catalog.list())
                .extracting(TargetSiteOnboardingProfileTemplate::profileKey)
                .contains("greenhouse_bitso");
    }

    @Test
    @DisplayName("should return detailed onboarding template for known profile key")
    void shouldReturnDetailedOnboardingTemplateForKnownProfileKey() {
        TargetSiteOnboardingProfileTemplate template = catalog.get("greenhouse_bitso");

        assertThat(template.profileKey()).isEqualTo("greenhouse_bitso");
        assertThat(template.sourceFamily()).isEqualTo("GREENHOUSE");
        assertThat(template.targetSite().getSiteCode()).isEqualTo("greenhouse_bitso");
        assertThat(template.checklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
    }

    @Test
    @DisplayName("should fail when onboarding profile key is unknown")
    void shouldFailWhenOnboardingProfileKeyIsUnknown() {
        assertThatThrownBy(() -> catalog.get("unknown_profile"))
                .isInstanceOf(TargetSiteOnboardingProfileNotFoundException.class)
                .hasMessage("Target site onboarding profile not found: unknown_profile");
    }
}
