package com.campos.webscraper.shared;

import java.util.List;
import java.util.Objects;

/**
 * Raised when a target site cannot be activated because onboarding compliance is incomplete.
 */
public class TargetSiteActivationBlockedException extends RuntimeException {

    private final Long siteId;
    private final List<String> blockingReasons;

    public TargetSiteActivationBlockedException(Long siteId, List<String> blockingReasons) {
        super("Target site activation blocked: " + siteId);
        this.siteId = Objects.requireNonNull(siteId, "siteId must not be null");
        this.blockingReasons = List.copyOf(blockingReasons);
    }

    public Long getSiteId() {
        return siteId;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }
}
