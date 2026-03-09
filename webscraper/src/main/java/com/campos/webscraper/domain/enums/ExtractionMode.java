package com.campos.webscraper.domain.enums;

/**
 * Define o modo de extração utilizado por uma {@code JobScraperStrategy} para
 * obter dados de um site-alvo.
 *
 * <p>A escolha do modo segue a hierarquia API-first definida em ADR001:
 * <ol>
 *   <li>{@link #API} — sempre preferido quando disponível.</li>
 *   <li>{@link #STATIC_HTML} — para sites Tipo A (HTML renderizado no servidor).</li>
 *   <li>{@link #DYNAMIC_HTML} — para sites Tipo B (AJAX parcial).</li>
 *   <li>{@link #BROWSER_AUTOMATION} — último recurso para sites Tipo C (SPA/JS-heavy).</li>
 * </ol>
 */
public enum ExtractionMode {

    /** Consumo de API oficial ou pública (REST/JSON). Estratégia prioritária. */
    API,

    /** Parsing de HTML estático com jsoup — site renderiza o conteúdo no servidor. */
    STATIC_HTML,

    /** OkHttp + jsoup com chamadas auxiliares às APIs internas do site (AJAX parcial). */
    DYNAMIC_HTML,

    /** Browser automation via Playwright — apenas para SPAs comprovadamente JS-heavy (Tipo C). */
    BROWSER_AUTOMATION
}
