package com.campos.webscraper.shared;

import com.campos.webscraper.domain.model.JobPostingEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Calculates a stable SHA-256 fingerprint for a job posting using canonical business fields.
 */
public class JobPostingFingerprintCalculator {

    /**
     * Calculates the canonical fingerprint hash for a private-sector job posting.
     */
    public String calculate(JobPostingEntity posting) {
        Objects.requireNonNull(posting, "posting must not be null");
        Objects.requireNonNull(posting.getTargetSite(), "targetSite must not be null");
        Objects.requireNonNull(posting.getTargetSite().getSiteCode(), "targetSite.siteCode must not be null");
        Objects.requireNonNull(posting.getCanonicalUrl(), "canonicalUrl must not be null");
        Objects.requireNonNull(posting.getTitle(), "title must not be null");
        Objects.requireNonNull(posting.getCompany(), "company must not be null");
        Objects.requireNonNull(posting.getPublishedAt(), "publishedAt must not be null");

        String canonical = String.join("|",
                normalize(posting.getTargetSite().getSiteCode()),
                normalize(posting.getExternalId()),
                normalize(posting.getCanonicalUrl()),
                normalize(posting.getTitle()),
                normalize(posting.getCompany()),
                normalize(posting.getPublishedAt())
        );

        return sha256(canonical);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalize(LocalDate value) {
        return value == null ? "" : value.toString();
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
