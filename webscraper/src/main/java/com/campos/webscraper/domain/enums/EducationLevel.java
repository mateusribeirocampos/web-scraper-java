package com.campos.webscraper.domain.enums;

/**
 * Nível de escolaridade mínimo exigido para uma vaga de concurso público.
 *
 * <p>Utilizado em {@code PublicContestPostingEntity.educationLevel}.
 * A ordem da declaração representa a progressão do nível de formação.
 */
public enum EducationLevel {

    /** Ensino fundamental completo. */
    FUNDAMENTAL,

    /** Ensino médio completo (2° grau). */
    MEDIO,

    /** Curso técnico / nível técnico profissionalizante. */
    TECNICO,

    /** Ensino superior completo (graduação / nível superior). */
    SUPERIOR,

    /** Especialização, mestrado ou doutorado. */
    POS_GRADUACAO
}
