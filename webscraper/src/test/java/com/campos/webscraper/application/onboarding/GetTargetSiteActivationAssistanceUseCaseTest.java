package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("GetTargetSiteActivationAssistanceUseCase")
class GetTargetSiteActivationAssistanceUseCaseTest {

    @Mock
    private TargetSiteRepository targetSiteRepository;

    private final TargetSiteOnboardingProfileCatalog catalog = new TargetSiteOnboardingProfileCatalog();
    private final TargetSiteOnboardingValidator validator = new TargetSiteOnboardingValidator();

    @Test
    @DisplayName("should return curated activation assistance when target site matches known profile")
    void shouldReturnCuratedActivationAssistanceWhenTargetSiteMatchesKnownProfile() {
        when(targetSiteRepository.findById(7L)).thenReturn(Optional.of(greenhouseBitso()));

        GetTargetSiteActivationAssistanceUseCase useCase = new GetTargetSiteActivationAssistanceUseCase(
                targetSiteRepository,
                catalog,
                validator
        );

        TargetSiteActivationAssistance assistance = useCase.execute(7L);

        assertThat(assistance.profileKey()).isEqualTo("greenhouse_bitso");
        assertThat(assistance.assistanceSource()).isEqualTo(ActivationAssistanceSource.CURATED_PROFILE);
        assertThat(assistance.suggestedChecklist().officialApiEndpointUrl())
                .isEqualTo("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true");
        assertThat(assistance.productionReadyIfActivatedNow()).isTrue();
        assertThat(assistance.blockingReasonsIfActivatedNow()).isEmpty();
    }

    @Test
    @DisplayName("should derive activation assistance draft when target site has no curated profile")
    void shouldDeriveActivationAssistanceDraftWhenTargetSiteHasNoCuratedProfile() {
        when(targetSiteRepository.findById(9L)).thenReturn(Optional.of(customApiSite()));

        GetTargetSiteActivationAssistanceUseCase useCase = new GetTargetSiteActivationAssistanceUseCase(
                targetSiteRepository,
                catalog,
                validator
        );

        TargetSiteActivationAssistance assistance = useCase.execute(9L);

        assertThat(assistance.profileKey()).isNull();
        assertThat(assistance.assistanceSource()).isEqualTo(ActivationAssistanceSource.DERIVED_FROM_TARGET_SITE);
        assertThat(assistance.suggestedChecklist().robotsTxtUrl()).isEqualTo("https://careers.example.com/robots.txt");
        assertThat(assistance.suggestedChecklist().officialApiEndpointUrl())
                .isEqualTo("https://careers.example.com/api/jobs");
        assertThat(assistance.suggestedChecklist().officialApiChecked()).isFalse();
        assertThat(assistance.suggestedChecklist().strategySupportVerified()).isFalse();
        assertThat(assistance.suggestedChecklist().legalCategory()).isEqualTo(OnboardingLegalCategory.API_OFICIAL);
        assertThat(assistance.productionReadyIfActivatedNow()).isFalse();
        assertThat(assistance.blockingReasonsIfActivatedNow())
                .contains(
                        "robots.txt not reviewed",
                        "terms of service not reviewed",
                        "official API not checked",
                        "scraper strategy not implemented for target site"
                );
    }

    private static TargetSiteEntity greenhouseBitso() {
        return TargetSiteEntity.builder()
                .id(7L)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-24T18:00:00Z"))
                .updatedAt(Instant.parse("2026-03-24T18:30:00Z"))
                .build();
    }

    private static TargetSiteEntity customApiSite() {
        return TargetSiteEntity.builder()
                .id(9L)
                .siteCode("custom_api_site")
                .displayName("Custom API Site")
                .baseUrl("https://careers.example.com/api/jobs")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-24T18:00:00Z"))
                .updatedAt(Instant.parse("2026-03-24T18:30:00Z"))
                .build();
    }
}
