package com.campos.webscraper.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de invariantes do value object ScrapeResult.
 *
 * <p>ScrapeResult é genérico e imutável (record). Testa:
 * <ul>
 *   <li>Factory {@code success()} cria resultado com itens e success=true.</li>
 *   <li>Factory {@code failure()} cria resultado com lista vazia e success=false.</li>
 *   <li>Resultado de falha SEM errorMessage lança exceção.</li>
 *   <li>Lista de itens não pode ser modificada externamente (imutabilidade).</li>
 *   <li>Campos obrigatórios: source, scrapedAt.</li>
 * </ul>
 *
 * <p>Ciclo TDD: testes escritos ANTES de ScrapeResult existir (fase RED).
 */
@Tag("unit")
@DisplayName("ScrapeResult — invariantes de value object")
class ScrapeResultTest {

    private static final String SOURCE = "indeed-br";

    // -------------------------------------------------------------------------
    // Factory success()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("success() deve criar resultado com itens e success=true")
    void successFactoryShouldCreateSuccessfulResult() {
        List<String> items = List.of("vaga1", "vaga2");
        ScrapeResult<String> result = ScrapeResult.success(items, SOURCE);

        assertThat(result.success()).isTrue();
        assertThat(result.items()).containsExactly("vaga1", "vaga2");
        assertThat(result.source()).isEqualTo(SOURCE);
        assertThat(result.scrapedAt()).isNotNull();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("success() com lista vazia ainda deve ter success=true")
    void successWithEmptyListShouldStillBeSuccess() {
        ScrapeResult<String> result = ScrapeResult.success(List.of(), SOURCE);

        assertThat(result.success()).isTrue();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("success() deve registrar scrapedAt próximo do momento atual")
    void successShouldRecordScrapedAtNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ScrapeResult<String> result = ScrapeResult.success(List.of(), SOURCE);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(result.scrapedAt()).isBetween(before, after);
    }

    // -------------------------------------------------------------------------
    // Factory failure()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("failure() deve criar resultado com items vazio e success=false")
    void failureFactoryShouldCreateFailedResult() {
        ScrapeResult<String> result = ScrapeResult.failure(SOURCE, "Timeout ao conectar");

        assertThat(result.success()).isFalse();
        assertThat(result.items()).isEmpty();
        assertThat(result.source()).isEqualTo(SOURCE);
        assertThat(result.errorMessage()).isEqualTo("Timeout ao conectar");
        assertThat(result.scrapedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Invariantes de falha
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resultado com success=false e errorMessage null deve lançar IllegalArgumentException")
    void shouldThrowWhenSuccessIsFalseButNoErrorMessage() {
        assertThatThrownBy(() ->
                new ScrapeResult<>(List.of(), SOURCE, LocalDateTime.now(), false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("errorMessage");
    }

    @Test
    @DisplayName("resultado com success=false e errorMessage blank deve lançar IllegalArgumentException")
    void shouldThrowWhenSuccessIsFalseButErrorMessageIsBlank() {
        assertThatThrownBy(() ->
                new ScrapeResult<>(List.of(), SOURCE, LocalDateTime.now(), false, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("errorMessage");
    }

    // -------------------------------------------------------------------------
    // Campos obrigatórios
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar NullPointerException quando source for null")
    void shouldThrowWhenSourceIsNull() {
        assertThatThrownBy(() ->
                new ScrapeResult<>(List.of(), null, LocalDateTime.now(), true, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("source");
    }

    @Test
    @DisplayName("deve lançar NullPointerException quando scrapedAt for null")
    void shouldThrowWhenScrapedAtIsNull() {
        assertThatThrownBy(() ->
                new ScrapeResult<>(List.of(), SOURCE, null, true, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("scrapedAt");
    }

    // -------------------------------------------------------------------------
    // Imutabilidade da lista
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("lista de itens deve ser imutável — modificação deve lançar exceção")
    void itemListShouldBeUnmodifiable() {
        ScrapeResult<String> result = ScrapeResult.success(List.of("vaga1"), SOURCE);

        assertThatThrownBy(() -> result.items().add("vaga2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("items null deve ser tratado como lista vazia")
    void nullItemsShouldBeTreatedAsEmptyList() {
        ScrapeResult<String> result = new ScrapeResult<>(null, SOURCE, LocalDateTime.now(), true, null);

        assertThat(result.items()).isEmpty();
    }
}
