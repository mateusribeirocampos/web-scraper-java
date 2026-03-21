package com.campos.webscraper.domain.enums;

/**
 * Search profiles used to turn recent job postings into user-meaningful result sets.
 */
public enum JobPostingSearchProfile {

    /**
     * Default product profile: recent private-sector postings aligned with Java / Spring / Kotlin backend roles
     * and excluding obvious talent-pool or senior-only noise.
     */
    JAVA_JUNIOR_BACKEND,

    /**
     * Returns recent postings without additional relevance filtering.
     */
    UNFILTERED
}
