package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.interfaces.dto.TargetSiteOnboardingProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("GetTargetSiteOnboardingProfileUseCase")
class GetTargetSiteOnboardingProfileUseCaseTest {

    private final GetTargetSiteOnboardingProfileUseCase useCase =
            new GetTargetSiteOnboardingProfileUseCase(new TargetSiteOnboardingProfileCatalog());

    @Test
    @DisplayName("should expose board token for Workday onboarding profiles")
    void shouldExposeBoardTokenForWorkdayOnboardingProfiles() {
        TargetSiteOnboardingProfileResponse response = useCase.execute("airbus_helibras_workday");

        assertThat(response.profileKey()).isEqualTo("airbus_helibras_workday");
        assertThat(response.sourceFamily()).isEqualTo("WORKDAY");
        assertThat(response.boardToken()).isEqualTo("Airbus");
        assertThat(response.sourceIdentifier()).isEqualTo("Airbus");
    }
}
