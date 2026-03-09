package com.campos.webscraper.shared;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa a resposta HTTP recebida pelo {@code JobFetcher} após executar
 * um {@code FetchRequest}.
 *
 * <p>É um value object imutável (record Java). Encapsula o conteúdo HTML
 * bruto, o código de status HTTP e metadados da busca.
 *
 * <p>Invariantes:
 * <ul>
 *   <li>{@code url} não pode ser null — representa a URL efetiva após redirecionamentos.</li>
 *   <li>{@code fetchedAt} não pode ser null.</li>
 *   <li>{@code htmlContent} null é normalizado para string vazia.</li>
 * </ul>
 *
 * <p>Métodos utilitários:
 * <ul>
 *   <li>{@link #isSuccess()} — {@code true} para status HTTP 2xx.</li>
 *   <li>{@link #isEmpty()} — {@code true} se o conteúdo HTML está ausente.</li>
 * </ul>
 *
 * @param url         URL efetiva da página (pode diferir da URL solicitada após redirecionamentos)
 * @param htmlContent conteúdo HTML da resposta (nunca null — normalizado para "" se ausente)
 * @param statusCode  código de status HTTP (ex.: 200, 404, 500)
 * @param contentType valor do cabeçalho Content-Type (pode ser null)
 * @param fetchedAt   timestamp do momento em que a resposta foi recebida
 */
public record FetchedPage(
        String url,
        String htmlContent,
        int statusCode,
        String contentType,
        LocalDateTime fetchedAt
) {

    /**
     * Construtor compacto com validação e normalização.
     */
    public FetchedPage {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
        // Normaliza null para string vazia — evita NullPointerException nos parsers
        htmlContent = (htmlContent == null) ? "" : htmlContent;
    }

    // =========================================================================
    // Métodos utilitários
    // =========================================================================

    /**
     * Retorna {@code true} se o status HTTP está na faixa 2xx (sucesso).
     *
     * @return {@code true} para status entre 200 e 299 (inclusive)
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Retorna {@code true} se o conteúdo HTML está ausente ou contém apenas
     * espaços em branco.
     *
     * @return {@code true} se não há conteúdo a ser processado
     */
    public boolean isEmpty() {
        return htmlContent == null || htmlContent.isBlank();
    }
}
