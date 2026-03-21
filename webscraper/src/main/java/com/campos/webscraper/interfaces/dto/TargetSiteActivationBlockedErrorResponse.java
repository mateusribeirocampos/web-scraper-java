package com.campos.webscraper.interfaces.dto;

import java.util.List;

/**
 * Error payload returned when onboarding compliance blocks a target-site activation.
 */
public record TargetSiteActivationBlockedErrorResponse(
        String message,
        List<String> blockingReasons
) {
}
