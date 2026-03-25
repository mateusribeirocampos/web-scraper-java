package com.campos.webscraper.interfaces.dto;

import java.util.List;

public record TargetSiteActivationAssistanceResponse(
        Long siteId,
        String siteCode,
        String profileKey,
        String assistanceSource,
        boolean productionReadyIfActivatedNow,
        List<String> blockingReasonsIfActivatedNow,
        List<String> notes,
        String robotsTxtUrl,
        boolean robotsTxtReviewed,
        boolean robotsTxtAllowsScraping,
        String termsOfServiceUrl,
        boolean termsReviewed,
        boolean termsAllowScraping,
        boolean officialApiChecked,
        String officialApiEndpointUrl,
        boolean strategySupportVerified,
        String businessJustification,
        String rateLimitProfile,
        String legalCategory,
        String owner,
        String authenticationStatus,
        String discoveryEvidence
) {
}
