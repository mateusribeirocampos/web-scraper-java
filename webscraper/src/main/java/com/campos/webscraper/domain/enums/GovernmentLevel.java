package com.campos.webscraper.domain.enums;

/**
 * Esfera governamental do órgão que abre o concurso público.
 *
 * <p>Utilizado em {@code PublicContestPostingEntity.governmentLevel} para
 * classificar e filtrar concursos por esfera. A integração inicial (ADR009,
 * Iteração 4) foca na esfera {@link #FEDERAL} via DOU API.
 */
public enum GovernmentLevel {

    /**
     * Esfera federal — órgãos da administração pública direta e indireta da União.
     * Fonte primária: DOU (Diário Oficial da União) via API gov.br.
     */
    FEDERAL,

    /**
     * Esfera estadual — governo dos estados e do Distrito Federal.
     */
    ESTADUAL,

    /**
     * Esfera municipal — prefeituras e câmaras municipais.
     */
    MUNICIPAL,

    /**
     * Autarquias, fundações públicas, empresas públicas e sociedades de economia mista
     * que realizam concursos independentemente de sua esfera de vinculação.
     */
    AUTARCHY
}
