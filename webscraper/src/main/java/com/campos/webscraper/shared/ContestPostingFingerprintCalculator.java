package com.campos.webscraper.shared;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Calculates a stable SHA-256 fingerprint for public contest postings using canonical business fields.
 */
public class ContestPostingFingerprintCalculator {

    /**
     * Calculates the canonical fingerprint hash for a public contest posting.
     */
    public String calculate(PublicContestPostingEntity posting) {
        Objects.requireNonNull(posting, "posting must not be null");
        Objects.requireNonNull(posting.getTargetSite(), "targetSite must not be null");
        Objects.requireNonNull(posting.getTargetSite().getSiteCode(), "targetSite.siteCode must not be null");
        Objects.requireNonNull(posting.getContestName(), "contestName must not be null");
        Objects.requireNonNull(posting.getOrganizer(), "organizer must not be null");

        String canonical = String.join("|",
                normalize(posting.getTargetSite().getSiteCode()),
                normalize(posting.getExternalId()),
                normalize(posting.getContestName()),
                normalize(posting.getOrganizer()),
                normalizeOrFallback(posting.getRegistrationEndDate(), "no-deadline")
        );

        return sha256(canonical);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeOrFallback(LocalDate value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
