package com.campos.webscraper.application.onboarding;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Generic recent posting sample used by the onboarding operational check across private jobs and public contests.
 */
public record OnboardingRecentPostingSample(
        Long id,
        String title,
        String organization,
        String canonicalUrl,
        LocalDate publishedAt
) {

    public OnboardingRecentPostingSample {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(organization, "organization must not be null");
        Objects.requireNonNull(canonicalUrl, "canonicalUrl must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    }
}
