package com.campos.webscraper.domain.enums;

/**
 * Representa o resultado da análise legal de um site-alvo para scraping
 * (robots.txt + Termos de Serviço), conforme checklist definido em ADR002.
 *
 * <p><strong>Regra de produção (ADR002, Seção 3):</strong> nenhum site pode ser
 * ativado em produção com status diferente de {@link #APPROVED}.
 *
 * <ul>
 *   <li>{@link #APPROVED} — análise concluída, scraping permitido (robots.txt + ToS ok).</li>
 *   <li>{@link #PENDING_REVIEW} — análise ainda não realizada ou incompleta.</li>
 *   <li>{@link #SCRAPING_PROIBIDO} — robots.txt ou ToS proíbem explicitamente scraping.
 *       Ex.: LinkedIn, Catho, Glassdoor.</li>
 * </ul>
 */
public enum LegalStatus {

    /** Análise jurídica concluída — scraping permitido. Site pode ir para produção. */
    APPROVED,

    /** Análise pendente — site não pode ser ativado em produção. */
    PENDING_REVIEW,

    /** Scraping explicitamente proibido por robots.txt ou Termos de Serviço. */
    SCRAPING_PROIBIDO
}
