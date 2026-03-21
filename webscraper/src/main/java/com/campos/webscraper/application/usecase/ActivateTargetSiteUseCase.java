package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.onboarding.TargetSiteOnboardingDecision;
import com.campos.webscraper.application.onboarding.TargetSiteOnboardingValidator;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.interfaces.dto.TargetSiteActivationRequest;
import com.campos.webscraper.shared.TargetSiteActivationBlockedException;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Applies onboarding evidence to a persisted target site and only enables it when compliance is complete.
 */
@Component
public class ActivateTargetSiteUseCase {

    private final TargetSiteRepository targetSiteRepository;
    private final TargetSiteOnboardingValidator targetSiteOnboardingValidator;
    private final Clock clock;

    public ActivateTargetSiteUseCase(
            TargetSiteRepository targetSiteRepository,
            TargetSiteOnboardingValidator targetSiteOnboardingValidator,
            Clock clock
    ) {
        this.targetSiteRepository = Objects.requireNonNull(targetSiteRepository, "targetSiteRepository must not be null");
        this.targetSiteOnboardingValidator = Objects.requireNonNull(
                targetSiteOnboardingValidator,
                "targetSiteOnboardingValidator must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public TargetSiteEntity execute(Long siteId, TargetSiteActivationRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        TargetSiteEntity persistedSite = targetSiteRepository.findById(siteId)
                .orElseThrow(() -> new TargetSiteNotFoundException(siteId));

        TargetSiteOnboardingDecision decision = targetSiteOnboardingValidator.assess(
                persistedSite,
                request.toChecklist()
        );

        TargetSiteEntity reconciledSite = copyWithUpdatedAt(decision.targetSite(), Instant.now(clock));
        targetSiteRepository.save(reconciledSite);

        if (!decision.productionReady()) {
            throw new TargetSiteActivationBlockedException(siteId, decision.blockingReasons());
        }

        return reconciledSite;
    }

    private static TargetSiteEntity copyWithUpdatedAt(TargetSiteEntity source, Instant updatedAt) {
        return TargetSiteEntity.builder()
                .id(source.getId())
                .siteCode(source.getSiteCode())
                .displayName(source.getDisplayName())
                .baseUrl(source.getBaseUrl())
                .siteType(source.getSiteType())
                .extractionMode(source.getExtractionMode())
                .jobCategory(source.getJobCategory())
                .legalStatus(source.getLegalStatus())
                .selectorBundleVersion(source.getSelectorBundleVersion())
                .enabled(source.isEnabled())
                .createdAt(source.getCreatedAt())
                .updatedAt(updatedAt)
                .build();
    }
}
