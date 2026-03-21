package com.campos.webscraper.interfaces.dto;

/**
 * Response returned after a successful target-site activation.
 */
public record TargetSiteActivationResponse(
        Long siteId,
        String siteCode,
        boolean enabled,
        String legalStatus
) {
}
