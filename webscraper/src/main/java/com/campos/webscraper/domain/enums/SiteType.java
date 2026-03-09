package com.campos.webscraper.domain.enums;

/**
 * Classifica o tipo técnico de um site-alvo, determinando a estratégia de
 * extração a ser usada (ver ADR002 e ADR004).
 *
 * <ul>
 *   <li>{@link #TYPE_A} — HTML estático: conteúdo renderizado no servidor.
 *       Estratégia: jsoup com seletores CSS.</li>
 *   <li>{@link #TYPE_B} — HTML semi-dinâmico: algumas seções carregadas via AJAX,
 *       mas grande parte disponível no HTML inicial. Estratégia: OkHttp + jsoup
 *       com chamadas adicionais às APIs internas.</li>
 *   <li>{@link #TYPE_C} — JS-heavy (Single Page Application): conteúdo renderizado
 *       inteiramente pelo browser. Estratégia: Playwright (browser automation).
 *       Usar apenas após comprovação — é a estratégia mais custosa.</li>
 *   <li>{@link #TYPE_D} — Autenticado: requer login para acessar o conteúdo.
 *       Excluído da fase 1 do projeto.</li>
 *   <li>{@link #TYPE_E} — API oficial ou pública: endpoint documentado disponível.
 *       Estratégia prioritária (ADR001: API-first). Ex.: Indeed MCP, DOU API.</li>
 * </ul>
 */
public enum SiteType {

    /** HTML estático renderizado no servidor — extração via jsoup. */
    TYPE_A,

    /** HTML semi-dinâmico com AJAX parcial — jsoup + chamadas às APIs internas. */
    TYPE_B,

    /** JavaScript-heavy / SPA — browser automation via Playwright (somente se comprovado). */
    TYPE_C,

    /** Site autenticado — requer login; excluído da fase 1 do projeto. */
    TYPE_D,

    /** API oficial ou pública disponível — estratégia de extração prioritária (API-first). */
    TYPE_E
}
