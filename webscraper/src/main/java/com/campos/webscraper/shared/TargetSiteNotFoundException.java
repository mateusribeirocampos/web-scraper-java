package com.campos.webscraper.shared;

/**
 * Raised when a requested target site id does not exist.
 */
public class TargetSiteNotFoundException extends RuntimeException {

    public TargetSiteNotFoundException(Long siteId) {
        super("Target site not found: " + siteId);
    }
}
