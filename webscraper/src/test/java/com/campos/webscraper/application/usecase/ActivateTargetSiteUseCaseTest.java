package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.onboarding.OnboardingLegalCategory;
import com.campos.webscraper.application.onboarding.SiteOnboardingChecklist;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.shared.TargetSiteActivationBlockedException;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivateTargetSiteUseCase")
class ActivateTargetSiteUseCaseTest {

    @Mock
    private TargetSiteRepository targetSiteRepository;

    @Test
    @DisplayName("should persist approved and enabled target site when checklist is complete")
    void shouldPersistApprovedAndEnabledTargetSiteWhenChecklistIsComplete() {
        TargetSiteEntity site = buildTargetSite(7L, false, LegalStatus.PENDING_REVIEW);
        when(targetSiteRepository.findById(7L)).thenReturn(Optional.of(site));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivateTargetSiteUseCase useCase = new ActivateTargetSiteUseCase(
                targetSiteRepository,
                new com.campos.webscraper.application.onboarding.TargetSiteOnboardingValidator(),
                Clock.fixed(Instant.parse("2026-03-21T18:00:00Z"), ZoneOffset.UTC)
        );

        TargetSiteEntity activated = useCase.execute(7L, request(completeChecklist()));

        assertThat(activated.getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(activated.isEnabled()).isTrue();
        verify(targetSiteRepository).save(any(TargetSiteEntity.class));
    }

    @Test
    @DisplayName("should reject activation but persist the blocked compliance status")
    void shouldRejectActivationButPersistTheBlockedComplianceStatus() {
        TargetSiteEntity site = buildTargetSite(7L, false, LegalStatus.PENDING_REVIEW);
        when(targetSiteRepository.findById(7L)).thenReturn(Optional.of(site));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivateTargetSiteUseCase useCase = new ActivateTargetSiteUseCase(
                targetSiteRepository,
                new com.campos.webscraper.application.onboarding.TargetSiteOnboardingValidator(),
                Clock.fixed(Instant.parse("2026-03-21T18:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> useCase.execute(7L, request(incompleteChecklist())))
                .isInstanceOf(TargetSiteActivationBlockedException.class)
                .hasMessage("Target site activation blocked: 7");

        ArgumentCaptor<TargetSiteEntity> captor = ArgumentCaptor.forClass(TargetSiteEntity.class);
        verify(targetSiteRepository).save(captor.capture());
        assertThat(captor.getValue().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(captor.getValue().isEnabled()).isFalse();
        assertThat(captor.getValue().getUpdatedAt()).isEqualTo(Instant.parse("2026-03-21T18:00:00Z"));
    }

    @Test
        @DisplayName("should fail when target site does not exist")
    void shouldFailWhenTargetSiteDoesNotExist() {
        when(targetSiteRepository.findById(99L)).thenReturn(Optional.empty());

        ActivateTargetSiteUseCase useCase = new ActivateTargetSiteUseCase(
                targetSiteRepository,
                new com.campos.webscraper.application.onboarding.TargetSiteOnboardingValidator(),
                Clock.fixed(Instant.parse("2026-03-21T18:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> useCase.execute(99L, request(completeChecklist())))
                .isInstanceOf(TargetSiteNotFoundException.class)
                .hasMessage("Target site not found: 99");
    }

    private static com.campos.webscraper.interfaces.dto.TargetSiteActivationRequest request(
            SiteOnboardingChecklist checklist
    ) {
        return new com.campos.webscraper.interfaces.dto.TargetSiteActivationRequest(
                checklist.robotsTxtUrl(),
                checklist.robotsTxtReviewed(),
                checklist.robotsTxtAllowsScraping(),
                checklist.termsOfServiceUrl(),
                checklist.termsReviewed(),
                checklist.termsAllowScraping(),
                checklist.officialApiChecked(),
                checklist.officialApiEndpointUrl(),
                checklist.strategySupportVerified(),
                checklist.businessJustification(),
                checklist.rateLimitProfile(),
                checklist.legalCategory().name(),
                checklist.owner(),
                checklist.authenticationStatus(),
                checklist.discoveryEvidence()
        );
    }

    private static TargetSiteEntity buildTargetSite(Long id, boolean enabled, LegalStatus legalStatus) {
        return TargetSiteEntity.builder()
                .id(id)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards.greenhouse.io/bitso")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(legalStatus)
                .selectorBundleVersion("n/a")
                .enabled(enabled)
                .createdAt(Instant.parse("2026-03-21T17:00:00Z"))
                .updatedAt(Instant.parse("2026-03-21T17:00:00Z"))
                .build();
    }

    private static SiteOnboardingChecklist completeChecklist() {
        return new SiteOnboardingChecklist(
                "https://boards.greenhouse.io/robots.txt",
                true,
                true,
                "",
                true,
                true,
                true,
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                true,
                "Private-sector Java/backend source.",
                "60 rpm conservative",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Greenhouse public API reviewed."
        );
    }

    private static SiteOnboardingChecklist incompleteChecklist() {
        return new SiteOnboardingChecklist(
                "https://boards.greenhouse.io/robots.txt",
                true,
                true,
                "",
                false,
                false,
                true,
                "",
                true,
                "Private-sector Java/backend source.",
                "60 rpm conservative",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Missing terms review."
        );
    }
}
