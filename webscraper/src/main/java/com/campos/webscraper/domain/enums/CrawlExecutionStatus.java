package com.campos.webscraper.domain.enums;

/**
 * Ciclo de vida de uma execução de crawl ({@code CrawlExecutionEntity}).
 *
 * <p>Cada execução nasce como {@code PENDING}, transita para {@code RUNNING} ao ser
 * consumida pelo worker, e termina em {@code SUCCEEDED}, {@code FAILED} ou
 * {@code DEAD_LETTER} após esgotamento das tentativas de retry configuradas
 * no Resilience4j (ADR006).
 *
 * <p>A ordem da declaração representa a progressão temporal natural de uma execução.
 */
public enum CrawlExecutionStatus {

    /**
     * Execução aguardando ser consumida pelo worker — estado inicial de toda execução.
     * Equivale a uma mensagem na fila {@code static-scrape-jobs} ou {@code api-jobs}.
     */
    PENDING,

    /**
     * Worker assumiu a execução e o crawl está em andamento.
     * Somente uma execução por job deve estar {@code RUNNING} simultaneamente.
     */
    RUNNING,

    /**
     * Execução concluída com sucesso — vagas foram coletadas e persistidas.
     * Contadores de itens extraídos e persistidos são registrados neste estado.
     */
    SUCCEEDED,

    /**
     * Execução falhou, mas ainda está dentro da política de retry configurada.
     * O Resilience4j irá reagendar a tentativa conforme o backoff exponencial (ADR006).
     */
    FAILED,

    /**
     * Todas as tentativas de retry foram esgotadas sem sucesso.
     * A execução é movida para a fila {@code dead-letter-jobs} para análise manual.
     */
    DEAD_LETTER
}
