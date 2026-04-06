package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Materializes a curated onboarding profile into a persisted target site, updating by site code when needed.
 */
@Component
public class BootstrapTargetSiteFromProfileUseCase {

    private final TargetSiteOnboardingProfileCatalog catalog;
    private final TargetSiteRepository targetSiteRepository;
    private final Clock clock;

    public BootstrapTargetSiteFromProfileUseCase(
            TargetSiteOnboardingProfileCatalog catalog,
            TargetSiteRepository targetSiteRepository,
            Clock clock
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.targetSiteRepository = Objects.requireNonNull(targetSiteRepository, "targetSiteRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public BootstrappedTargetSite execute(String profileKey) {
        TargetSiteOnboardingProfileTemplate template = catalog.get(profileKey);
        TargetSiteEntity templateSite = template.targetSite();
        Optional<TargetSiteEntity> existing = targetSiteRepository.findBySiteCode(templateSite.getSiteCode());
        Instant now = Instant.now(clock);

        BootstrapStatus bootstrapStatus = existing.isPresent() ? BootstrapStatus.UPDATED : BootstrapStatus.CREATED;
        TargetSiteEntity materialized = existing
                .map(persisted -> merge(templateSite, persisted, now))
                .orElseGet(() -> create(templateSite, now));

        try {
            TargetSiteEntity saved = targetSiteRepository.save(materialized);
            return new BootstrappedTargetSite(profileKey, bootstrapStatus, saved);
        } catch (DataIntegrityViolationException exception) {
            if (bootstrapStatus != BootstrapStatus.CREATED) {
                throw exception;
            }
            TargetSiteEntity persisted = targetSiteRepository.findBySiteCode(templateSite.getSiteCode())
                    .orElseThrow(() -> exception);
            return new BootstrappedTargetSite(profileKey, BootstrapStatus.UPDATED, persisted);
        }
    }

    private static TargetSiteEntity create(TargetSiteEntity templateSite, Instant now) {
        return TargetSiteEntity.builder()
                .siteCode(templateSite.getSiteCode())
                .displayName(templateSite.getDisplayName())
                .baseUrl(templateSite.getBaseUrl())
                .siteType(templateSite.getSiteType())
                .extractionMode(templateSite.getExtractionMode())
                .jobCategory(templateSite.getJobCategory())
                .legalStatus(templateSite.getLegalStatus())
                .selectorBundleVersion(templateSite.getSelectorBundleVersion())
                .enabled(templateSite.isEnabled())
                .createdAt(now)
                .updatedAt(null)
                .build();
    }

    private static TargetSiteEntity merge(TargetSiteEntity templateSite, TargetSiteEntity existing, Instant now) {
        boolean runnableConfigurationChanged = runnableConfigurationChanged(templateSite, existing);
        return TargetSiteEntity.builder()
                .id(existing.getId())
                .siteCode(existing.getSiteCode())
                .displayName(templateSite.getDisplayName())
                .baseUrl(templateSite.getBaseUrl())
                .siteType(templateSite.getSiteType())
                .extractionMode(templateSite.getExtractionMode())
                .jobCategory(templateSite.getJobCategory())
                .legalStatus(mergedLegalStatus(templateSite, existing, runnableConfigurationChanged))
                .selectorBundleVersion(templateSite.getSelectorBundleVersion())
                .enabled(mergedEnabled(templateSite, existing, runnableConfigurationChanged))
                .createdAt(existing.getCreatedAt())
                .updatedAt(now)
                .build();
    }

    private static LegalStatus mergedLegalStatus(
            TargetSiteEntity templateSite,
            TargetSiteEntity existing,
            boolean runnableConfigurationChanged
    ) {
        if (existing.getLegalStatus() == LegalStatus.SCRAPING_PROIBIDO) {
            return existing.getLegalStatus();
        }
        if (!runnableConfigurationChanged
                && existing.getLegalStatus() == LegalStatus.APPROVED
                && templateSite.getLegalStatus() == LegalStatus.PENDING_REVIEW) {
            return existing.getLegalStatus();
        }
        return templateSite.getLegalStatus();
    }

    private static boolean mergedEnabled(
            TargetSiteEntity templateSite,
            TargetSiteEntity existing,
            boolean runnableConfigurationChanged
    ) {
        LegalStatus mergedLegalStatus = mergedLegalStatus(templateSite, existing, runnableConfigurationChanged);
        if (mergedLegalStatus == LegalStatus.SCRAPING_PROIBIDO) {
            return false;
        }
        if (!runnableConfigurationChanged && existing.getLegalStatus() == LegalStatus.APPROVED) {
            return existing.isEnabled();
        }
        return templateSite.isEnabled();
    }

    private static boolean runnableConfigurationChanged(TargetSiteEntity templateSite, TargetSiteEntity existing) {
        return !Objects.equals(templateSite.getBaseUrl(), existing.getBaseUrl())
                || templateSite.getSiteType() != existing.getSiteType()
                || templateSite.getExtractionMode() != existing.getExtractionMode()
                || templateSite.getJobCategory() != existing.getJobCategory()
                || !Objects.equals(templateSite.getSelectorBundleVersion(), existing.getSelectorBundleVersion());
    }
}
