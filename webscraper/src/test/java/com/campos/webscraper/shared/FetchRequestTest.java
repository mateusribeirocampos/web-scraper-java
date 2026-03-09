package com.campos.webscraper.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de invariantes do value object FetchRequest.
 *
 * <p>FetchRequest representa o comando de busca HTTP a ser enviado ao fetcher.
 * Invariantes testadas:
 * <ul>
 *   <li>URL obrigatória — null ou blank lança exceção.</li>
 *   <li>Factory {@code of(url)} cria request com defaults (timeout 10s, followRedirects=true).</li>
 *   <li>Timeout deve ser positivo.</li>
 *   <li>Headers null são normalizados para mapa vazio.</li>
 *   <li>Headers externos não podem modificar o estado interno (imutabilidade).</li>
 * </ul>
 *
 * <p>Ciclo TDD: testes escritos ANTES de FetchRequest existir (fase RED).
 */
@Tag("unit")
@DisplayName("FetchRequest — invariantes de value object")
class FetchRequestTest {

    private static final String VALID_URL = "https://br.indeed.com/jobs?q=java";

    // -------------------------------------------------------------------------
    // Factory of(url)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("of(url) deve criar request com defaults sensatos")
    void factoryOfShouldCreateWithDefaults() {
        FetchRequest request = FetchRequest.of(VALID_URL);

        assertThat(request.url()).isEqualTo(VALID_URL);
        assertThat(request.headers()).isEmpty();
        assertThat(request.timeoutMs()).isEqualTo(10_000);
        assertThat(request.followRedirects()).isTrue();
    }

    @Test
    @DisplayName("of(url, headers) deve criar request com headers e demais defaults")
    void factoryOfWithHeadersShouldApplyThem() {
        Map<String, String> headers = Map.of("User-Agent", "Mozilla/5.0");
        FetchRequest request = FetchRequest.of(VALID_URL, headers);

        assertThat(request.headers()).containsEntry("User-Agent", "Mozilla/5.0");
        assertThat(request.timeoutMs()).isEqualTo(10_000);
        assertThat(request.followRedirects()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Criação com todos os parâmetros
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve criar FetchRequest com todos os campos válidos")
    void shouldCreateWithAllValidFields() {
        Map<String, String> headers = Map.of("Accept", "application/json");
        FetchRequest request = new FetchRequest(VALID_URL, headers, 5_000, false);

        assertThat(request.url()).isEqualTo(VALID_URL);
        assertThat(request.headers()).containsEntry("Accept", "application/json");
        assertThat(request.timeoutMs()).isEqualTo(5_000);
        assertThat(request.followRedirects()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Validação de URL
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar NullPointerException quando url for null")
    void shouldThrowWhenUrlIsNull() {
        assertThatThrownBy(() -> new FetchRequest(null, Map.of(), 10_000, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("url");
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando url for blank")
    void shouldThrowWhenUrlIsBlank() {
        assertThatThrownBy(() -> new FetchRequest("   ", Map.of(), 10_000, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando url for string vazia")
    void shouldThrowWhenUrlIsEmpty() {
        assertThatThrownBy(() -> new FetchRequest("", Map.of(), 10_000, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
    }

    // -------------------------------------------------------------------------
    // Validação de timeoutMs
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando timeoutMs for zero")
    void shouldThrowWhenTimeoutIsZero() {
        assertThatThrownBy(() -> new FetchRequest(VALID_URL, Map.of(), 0, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutMs");
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando timeoutMs for negativo")
    void shouldThrowWhenTimeoutIsNegative() {
        assertThatThrownBy(() -> new FetchRequest(VALID_URL, Map.of(), -1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutMs");
    }

    // -------------------------------------------------------------------------
    // Imutabilidade
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("headers null deve ser normalizado para mapa vazio")
    void nullHeadersShouldBeNormalizedToEmptyMap() {
        FetchRequest request = new FetchRequest(VALID_URL, null, 10_000, true);

        assertThat(request.headers()).isEmpty();
    }

    @Test
    @DisplayName("mapa de headers interno deve ser imutável")
    void internalHeadersShouldBeUnmodifiable() {
        FetchRequest request = FetchRequest.of(VALID_URL);

        assertThatThrownBy(() -> request.headers().put("X-Test", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("modificar o mapa original não deve alterar o FetchRequest")
    void externalHeaderMapModificationShouldNotAffectRequest() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html");
        FetchRequest request = FetchRequest.of(VALID_URL, headers);

        // modifica o mapa externo APÓS criar o record
        headers.put("X-Injected", "evil");

        assertThat(request.headers()).doesNotContainKey("X-Injected");
    }
}
