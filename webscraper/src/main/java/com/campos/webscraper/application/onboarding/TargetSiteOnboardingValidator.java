package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Applies the ADR002 onboarding checklist and derives the production-safe target-site status.
 */
@Component
public class TargetSiteOnboardingValidator {

    public TargetSiteOnboardingDecision assess(TargetSiteEntity targetSite, SiteOnboardingChecklist checklist) {
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(checklist, "checklist must not be null");

        List<String> blockingReasons = new ArrayList<>();

        requireNonBlank(checklist.robotsTxtUrl(), "robots.txt URL not documented", blockingReasons);
        requireTrue(checklist.robotsTxtReviewed(), "robots.txt not reviewed", blockingReasons);
        requireTrue(checklist.officialApiChecked(), "official API not checked", blockingReasons);
        requireNonBlank(checklist.businessJustification(), "business justification not documented", blockingReasons);
        requireNonBlank(checklist.rateLimitProfile(), "rate-limit profile not documented", blockingReasons);
        requireNonBlank(checklist.owner(), "owner/contact not documented", blockingReasons);
        requireNonBlank(checklist.authenticationStatus(), "authentication status not documented", blockingReasons);
        requireNonBlank(checklist.discoveryEvidence(), "discovery evidence not documented", blockingReasons);
        requireTrue(
                checklist.strategySupportVerified(),
                "scraper strategy not implemented for target site",
                blockingReasons
        );

        if (checklist.legalCategory() == OnboardingLegalCategory.API_OFICIAL) {
            requireNonBlank(
                    checklist.officialApiEndpointUrl(),
                    "official API endpoint not documented",
                    blockingReasons
            );
        }

        boolean apiExtraction = targetSite.getExtractionMode() == ExtractionMode.API;
        boolean apiSiteType = targetSite.getSiteType() == SiteType.TYPE_E;
        boolean apiBackedSite = apiExtraction && apiSiteType;
        boolean scrapingCategory = checklist.legalCategory() == OnboardingLegalCategory.SCRAPING_PERMITIDO
                || checklist.legalCategory() == OnboardingLegalCategory.SCRAPING_PROIBIDO;
        boolean htmlScrapingAccess = !apiBackedSite;

        boolean inconsistentApiMetadata = apiExtraction != apiSiteType;
        if (inconsistentApiMetadata) {
            blockingReasons.add("site extraction metadata is internally inconsistent");
        }

        boolean termsDocumented = checklist.termsReviewed() && checklist.termsOfServiceUrl() != null
                && !checklist.termsOfServiceUrl().isBlank();
        if (!checklist.termsReviewed()) {
            blockingReasons.add("terms of service not reviewed");
        } else if (scrapingCategory && !termsDocumented) {
            blockingReasons.add("terms of service not fully documented");
        }

        boolean apiCategoryMismatch = checklist.legalCategory() == OnboardingLegalCategory.API_OFICIAL && !apiBackedSite;
        if (apiCategoryMismatch) {
            blockingReasons.add("onboarding legal category does not match site extraction metadata");
        }
        if (apiBackedSite && scrapingCategory) {
            blockingReasons.add("onboarding legal category does not match site extraction metadata");
        }

        if (checklist.legalCategory() == OnboardingLegalCategory.API_OFICIAL
                && (inconsistentApiMetadata || apiCategoryMismatch)) {
            return new TargetSiteOnboardingDecision(
                    rebuildTargetSite(targetSite, LegalStatus.PENDING_REVIEW, false),
                    false,
                    blockingReasons
            );
        }

        boolean robotsExplicitlyDenyScraping = checklist.robotsTxtReviewed() && !checklist.robotsTxtAllowsScraping();
        boolean termsExplicitlyDenyScraping = checklist.termsReviewed() && !checklist.termsAllowScraping();

        boolean prohibited = checklist.legalCategory() == OnboardingLegalCategory.SCRAPING_PROIBIDO
                || termsExplicitlyDenyScraping
                || (htmlScrapingAccess && robotsExplicitlyDenyScraping);

        if (prohibited) {
            List<String> prohibitionReasons = new ArrayList<>(blockingReasons);
            prohibitionReasons.add("scraping explicitly blocked by onboarding evidence");
            return new TargetSiteOnboardingDecision(
                    rebuildTargetSite(targetSite, LegalStatus.SCRAPING_PROIBIDO, false),
                    false,
                    prohibitionReasons
            );
        }

        if (!blockingReasons.isEmpty()) {
            return new TargetSiteOnboardingDecision(
                    rebuildTargetSite(targetSite, LegalStatus.PENDING_REVIEW, false),
                    false,
                    blockingReasons
            );
        }

        return new TargetSiteOnboardingDecision(
                rebuildTargetSite(targetSite, LegalStatus.APPROVED, true),
                true,
                List.of()
        );
    }

    private static TargetSiteEntity rebuildTargetSite(TargetSiteEntity source, LegalStatus legalStatus, boolean enabled) {
        return TargetSiteEntity.builder()
                .id(source.getId())
                .siteCode(source.getSiteCode())
                .displayName(source.getDisplayName())
                .baseUrl(source.getBaseUrl())
                .siteType(source.getSiteType())
                .extractionMode(source.getExtractionMode())
                .jobCategory(source.getJobCategory())
                .legalStatus(legalStatus)
                .selectorBundleVersion(source.getSelectorBundleVersion())
                .enabled(enabled)
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }

    private static void requireTrue(boolean value, String message, List<String> blockingReasons) {
        if (!value) {
            blockingReasons.add(message);
        }
    }

    private static void requireNonBlank(String value, String message, List<String> blockingReasons) {
        if (value == null || value.isBlank()) {
            blockingReasons.add(message);
        }
    }
}
