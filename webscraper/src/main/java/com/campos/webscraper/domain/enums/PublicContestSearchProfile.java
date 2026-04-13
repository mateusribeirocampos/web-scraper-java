package com.campos.webscraper.domain.enums;

/**
 * Search profiles used to filter public contest postings by relevance to the user.
 *
 * <p>Unlike private-sector profiles that filter by seniority and tech stack,
 * public contest profiles filter by education/degree requirements and position
 * titles compatible with IT careers. The concept of "junior/senior" does not
 * exist in Brazilian public contests.
 */
public enum PublicContestSearchProfile {

    /**
     * Default product profile: contests requiring a degree in IT-related fields
     * (Ciência da Computação, Engenharia da Computação, Sistemas de Informação, etc.)
     * and positions compatible with IT careers (Analista de TI, Técnico em Informática, etc.).
     */
    TI_DEGREE_AND_ROLE,

    /**
     * Broader profile: contests with IT-compatible positions regardless of the
     * specific degree requirement, as long as the education level is SUPERIOR or TECNICO.
     */
    TI_ROLE_BROAD,

    /**
     * Returns all contests without additional relevance filtering.
     */
    UNFILTERED
}
