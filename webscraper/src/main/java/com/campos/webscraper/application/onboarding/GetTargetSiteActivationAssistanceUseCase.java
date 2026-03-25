package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class GetTargetSiteActivationAssistanceUseCase {

    private final TargetSiteRepository targetSiteRepository;
    private final TargetSiteOnboardingProfileCatalog targetSiteOnboardingProfileCatalog;
    private final TargetSiteOnboardingValidator targetSiteOnboardingValidator;

    public GetTargetSiteActivationAssistanceUseCase(
            TargetSiteRepository targetSiteRepository,
            TargetSiteOnboardingProfileCatalog targetSiteOnboardingProfileCatalog,
            TargetSiteOnboardingValidator targetSiteOnboardingValidator
    ) {
        this.targetSiteRepository = Objects.requireNonNull(targetSiteRepository, "targetSiteRepository must not be null");
        this.targetSiteOnboardingProfileCatalog = Objects.requireNonNull(
                targetSiteOnboardingProfileCatalog,
                "targetSiteOnboardingProfileCatalog must not be null"
        );
        this.targetSiteOnboardingValidator = Objects.requireNonNull(
                targetSiteOnboardingValidator,
                "targetSiteOnboardingValidator must not be null"
        );
    }

    public TargetSiteActivationAssistance execute(Long siteId) {
        TargetSiteEntity targetSite = targetSiteRepository.findById(siteId)
                .orElseThrow(() -> new TargetSiteNotFoundException(siteId));

        Optional<TargetSiteOnboardingProfileTemplate> matchingProfile =
                targetSiteOnboardingProfileCatalog.findBySiteCode(targetSite.getSiteCode());

        SiteOnboardingChecklist suggestedChecklist = matchingProfile
                .map(TargetSiteOnboardingProfileTemplate::checklist)
                .orElseGet(() -> deriveChecklist(targetSite));
        ActivationAssistanceSource assistanceSource = matchingProfile.isPresent()
                ? ActivationAssistanceSource.CURATED_PROFILE
                : ActivationAssistanceSource.DERIVED_FROM_TARGET_SITE;
        List<String> notes = matchingProfile
                .map(template -> curatedNotes(template.profileKey()))
                .orElseGet(() -> derivedNotes(targetSite, suggestedChecklist));

        TargetSiteOnboardingDecision decision = targetSiteOnboardingValidator.assess(targetSite, suggestedChecklist);
        return new TargetSiteActivationAssistance(
                targetSite.getId(),
                targetSite.getSiteCode(),
                matchingProfile.map(TargetSiteOnboardingProfileTemplate::profileKey).orElse(null),
                assistanceSource,
                suggestedChecklist,
                decision.productionReady(),
                decision.blockingReasons(),
                notes
        );
    }

    private static SiteOnboardingChecklist deriveChecklist(TargetSiteEntity targetSite) {
        String siteOrigin = deriveSiteOrigin(targetSite.getBaseUrl());
        boolean apiBackedSite = targetSite.getExtractionMode() == ExtractionMode.API
                && targetSite.getSiteType() == SiteType.TYPE_E;
        return new SiteOnboardingChecklist(
                siteOrigin == null ? "" : siteOrigin + "/robots.txt",
                false,
                false,
                "",
                false,
                false,
                false,
                apiBackedSite ? targetSite.getBaseUrl() : "",
                false,
                deriveBusinessJustification(targetSite),
                deriveRateLimitProfile(targetSite),
                deriveLegalCategory(targetSite),
                "",
                "",
                deriveDiscoveryEvidence(targetSite)
        );
    }

    private static String deriveSiteOrigin(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            if (port < 0) {
                return uri.getScheme() + "://" + uri.getHost();
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + port;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static OnboardingLegalCategory deriveLegalCategory(TargetSiteEntity targetSite) {
        if (targetSite.getExtractionMode() == ExtractionMode.API && targetSite.getSiteType() == SiteType.TYPE_E) {
            return OnboardingLegalCategory.API_OFICIAL;
        }
        if (targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST) {
            return OnboardingLegalCategory.DADOS_PUBLICOS;
        }
        return OnboardingLegalCategory.SCRAPING_PERMITIDO;
    }

    private static String deriveBusinessJustification(TargetSiteEntity targetSite) {
        if (targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST) {
            return "Fonte de concursos em tecnologia alinhada ao recorte atual do projeto.";
        }
        return "Fonte privada relevante para ampliar vagas de TI alinhadas ao recorte atual do projeto.";
    }

    private static String deriveRateLimitProfile(TargetSiteEntity targetSite) {
        return switch (targetSite.getExtractionMode()) {
            case API -> "API source: define provider-specific quota before activation";
            case STATIC_HTML -> "Static HTML source: start with 1 request every 5 seconds";
            case DYNAMIC_HTML, BROWSER_AUTOMATION -> "Browser-backed source: start with low-frequency serialized runs";
        };
    }

    private static String deriveDiscoveryEvidence(TargetSiteEntity targetSite) {
        return "Derived from persisted target-site metadata for " + targetSite.getDisplayName()
                + " (" + targetSite.getBaseUrl() + ").";
    }

    private static List<String> curatedNotes(String profileKey) {
        return List.of(
                "Checklist prefilled from curated onboarding profile: " + profileKey + ".",
                "Review robots.txt and terms evidence before calling the activation endpoint."
        );
    }

    private static List<String> derivedNotes(TargetSiteEntity targetSite, SiteOnboardingChecklist checklist) {
        List<String> notes = new ArrayList<>();
        notes.add("Checklist draft derived from persisted target-site metadata.");
        if (!checklist.robotsTxtUrl().isBlank()) {
            notes.add("robots.txt URL was derived from the target-site origin and still needs human review.");
        }
        if (!checklist.officialApiEndpointUrl().isBlank()) {
            notes.add("Official API endpoint was only suggested from targetSite.baseUrl and still needs evidence review.");
        } else {
            notes.add("Terms of service URL and legal evidence still require manual documentation before activation.");
        }
        notes.add("Strategy support remains unverified until the scraper implementation is explicitly confirmed.");
        if (targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST) {
            notes.add("Legal category was suggested as DADOS_PUBLICOS because the site is classified as public contests.");
        }
        return notes;
    }
}
