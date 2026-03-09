package com.campos.webscraper.domain.enums;

/**
 * Nível de senioridade esperado para uma vaga de emprego.
 *
 * <p>A plataforma filtra prioritariamente vagas com {@link #JUNIOR} e {@link #INTERN},
 * que são o foco do projeto (ver ADR001). Os demais níveis são coletados para
 * permitir consultas comparativas e análise de mercado.
 *
 * <p>A ordem da declaração representa a progressão natural de carreira.
 */
public enum SeniorityLevel {

    /** Nível júnior — foco principal do projeto (0 a 2 anos de experiência). */
    JUNIOR,

    /** Nível pleno — experiência intermediária (2 a 5 anos). */
    MID,

    /** Nível sênior — experiência avançada (5+ anos). */
    SENIOR,

    /** Tech Lead / Liderança técnica — responsabilidade sobre equipe ou arquitetura. */
    LEAD,

    /** Estagiário — estudante em formação, contrato regido pela Lei do Estágio. */
    INTERN
}
