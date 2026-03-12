package com.campos.webscraper.shared;

import java.util.Map;
import java.util.Objects;
import java.net.URI;

/**
 * Comando imutável que descreve uma requisição HTTP a ser executada pelo
 * {@code HttpJobFetcher} ou qualquer adaptador que implemente {@code JobFetcher}.
 *
 * <p>É um value object (record Java) com defaults sensatos acessíveis via
 * factory methods. Dois {@code FetchRequest} com os mesmos dados são iguais.
 *
 * <p>Invariantes:
 * <ul>
 *   <li>{@code url} não pode ser null nem blank.</li>
 *   <li>{@code timeoutMs} deve ser positivo (> 0).</li>
 *   <li>{@code headers} null é normalizado para mapa vazio.</li>
 *   <li>O mapa de headers é copiado defensivamente — modificações externas
 *       posteriores à criação não afetam o estado interno.</li>
 * </ul>
 *
 * @param url             URL alvo da requisição HTTP
 * @param headers         cabeçalhos HTTP adicionais (ex.: User-Agent, Accept)
 * @param timeoutMs       timeout da requisição em milissegundos (deve ser > 0)
 * @param followRedirects {@code true} para seguir redirecionamentos automaticamente
 * @param siteKey         chave lógica do site para políticas por fonte; quando null usa o host da URL
 */
public record FetchRequest(
        String url,
        Map<String, String> headers,
        int timeoutMs,
        boolean followRedirects,
        String siteKey
) {

    /** Timeout padrão usado pelos factory methods: 10 segundos. */
    private static final int DEFAULT_TIMEOUT_MS = 10_000;

    /**
     * Construtor compacto com validação e normalização.
     */
    public FetchRequest {
        Objects.requireNonNull(url, "url must not be null");
        if (url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be positive, got: " + timeoutMs);
        }
        // Normaliza null e cria cópia defensiva imutável
        headers = (headers == null) ? Map.of() : Map.copyOf(headers);
        siteKey = (siteKey == null || siteKey.isBlank()) ? null : siteKey.trim();
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Cria um {@code FetchRequest} com defaults: sem headers extras, timeout de
     * 10 segundos e followRedirects habilitado.
     *
     * @param url URL da requisição
     * @return {@code FetchRequest} com configuração mínima
     */
    public static FetchRequest of(String url) {
        return new FetchRequest(url, Map.of(), DEFAULT_TIMEOUT_MS, true, null);
    }

    /**
     * Cria um {@code FetchRequest} com headers personalizados e demais defaults.
     *
     * @param url     URL da requisição
     * @param headers cabeçalhos HTTP adicionais
     * @return {@code FetchRequest} com headers aplicados e demais valores padrão
     */
    public static FetchRequest of(String url, Map<String, String> headers) {
        return new FetchRequest(url, headers, DEFAULT_TIMEOUT_MS, true, null);
    }

    /**
     * Cria um {@code FetchRequest} com chave lógica do site e demais defaults.
     *
     * @param url URL da requisição
     * @param siteKey chave lógica do site
     * @return {@code FetchRequest} com siteKey e configuração padrão
     */
    public static FetchRequest of(String url, String siteKey) {
        return new FetchRequest(url, Map.of(), DEFAULT_TIMEOUT_MS, true, siteKey);
    }

    /**
     * Resolve a chave efetiva para políticas por site, usando {@code siteKey} explícito
     * quando informado e caindo para o host da URL como fallback.
     */
    public String rateLimitKey() {
        if (siteKey != null) {
            return siteKey;
        }
        return URI.create(url).getHost();
    }
}
