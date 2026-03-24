package com.campos.webscraper.application.onboarding;

/**
 * Raised when a requested operational onboarding profile is unknown.
 */
public class TargetSiteOnboardingProfileNotFoundException extends RuntimeException {

    public TargetSiteOnboardingProfileNotFoundException(String profileKey) {
        super("Target site onboarding profile not found: " + profileKey);
    }
}
