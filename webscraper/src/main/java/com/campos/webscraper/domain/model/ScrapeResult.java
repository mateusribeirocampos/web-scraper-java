package com.campos.webscraper.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Representa o resultado de uma operação de extração executada por uma
 * {@code JobScraperStrategy}.
 *
 * <p>É um value object genérico e imutável (record Java). O parâmetro de tipo
 * {@code T} representa o tipo do item extraído — normalmente um DTO ou
 * value object de domínio.
 *
 * <p>Invariantes validadas no construtor compacto:
 * <ul>
 *   <li>{@code source} não pode ser null.</li>
 *   <li>{@code scrapedAt} não pode ser null.</li>
 *   <li>Se {@code success} for {@code false}, {@code errorMessage} não pode ser
 *       null nem blank — a causa do erro deve ser informada.</li>
 *   <li>{@code items} null é normalizado para lista vazia.</li>
 *   <li>A lista de itens é tornada imutável ({@link List#copyOf}) — o caller
 *       não pode modificar o estado interno após a criação.</li>
 * </ul>
 *
 * <p>Prefira os factory methods {@link #success} e {@link #failure} em vez do
 * construtor canônico para criar instâncias de forma mais expressiva.
 *
 * @param <T> tipo do item extraído (ex.: {@code JobPostingDto}, {@code ContestDto})
 * @param items         lista imutável de itens extraídos (vazia em caso de falha)
 * @param source        identificador da fonte (siteCode ou URL base)
 * @param scrapedAt     timestamp UTC do momento da extração
 * @param success       {@code true} se a extração foi bem-sucedida
 * @param errorMessage  descrição do erro — obrigatória quando {@code success=false}
 */
public record ScrapeResult<T>(
        List<T> items,
        String source,
        LocalDateTime scrapedAt,
        boolean success,
        String errorMessage
) {

    /**
     * Construtor compacto com validação de invariantes e normalização.
     */
    public ScrapeResult {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(scrapedAt, "scrapedAt must not be null");

        // Normaliza null para lista vazia e garante imutabilidade
        items = (items == null) ? List.of() : List.copyOf(items);

        // Resultado de falha deve sempre informar a causa
        if (!success && (errorMessage == null || errorMessage.isBlank())) {
            throw new IllegalArgumentException(
                    "errorMessage must be provided and non-blank when success is false");
        }
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Cria um resultado de sucesso com os itens extraídos.
     *
     * @param items  lista de itens extraídos (pode ser vazia se nenhuma vaga foi encontrada)
     * @param source identificador da fonte
     * @param <T>    tipo do item extraído
     * @return {@code ScrapeResult} com {@code success=true} e {@code scrapedAt=now()}
     */
    public static <T> ScrapeResult<T> success(List<T> items, String source) {
        return new ScrapeResult<>(items, source, LocalDateTime.now(), true, null);
    }

    /**
     * Cria um resultado de falha com uma mensagem de erro descritiva.
     *
     * @param source       identificador da fonte onde ocorreu o erro
     * @param errorMessage descrição da causa — não pode ser null nem blank
     * @param <T>          tipo do item (lista será vazia)
     * @return {@code ScrapeResult} com {@code success=false} e {@code items=[]}
     */
    public static <T> ScrapeResult<T> failure(String source, String errorMessage) {
        return new ScrapeResult<>(List.of(), source, LocalDateTime.now(), false, errorMessage);
    }
}
