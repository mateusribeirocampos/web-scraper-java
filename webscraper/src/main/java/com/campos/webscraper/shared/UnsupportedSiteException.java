package com.campos.webscraper.shared;

/**
 * Thrown when no scraping strategy supports a given target site.
 */
public class UnsupportedSiteException extends RuntimeException {

    public UnsupportedSiteException(String message) {
        super(message);
    }
}
