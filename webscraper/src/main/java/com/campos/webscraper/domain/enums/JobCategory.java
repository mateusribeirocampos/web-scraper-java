package com.campos.webscraper.domain.enums;

/**
 * Categoriza a origem e a natureza de uma oportunidade de trabalho coletada
 * pela plataforma.
 *
 * <ul>
 *   <li>{@link #PRIVATE_SECTOR} — vagas em empresas privadas (Indeed, Gupy, etc.).
 *       Persistidas como {@code JobPostingEntity}.</li>
 *   <li>{@link #PUBLIC_CONTEST} — concursos públicos brasileiros (DOU, PCI Concursos).
 *       Persistidos como {@code PublicContestPostingEntity}.</li>
 * </ul>
 */
public enum JobCategory {

    /** Vaga de emprego no setor privado — entidade: JobPostingEntity. */
    PRIVATE_SECTOR,

    /** Concurso público brasileiro — entidade: PublicContestPostingEntity. */
    PUBLIC_CONTEST
}
