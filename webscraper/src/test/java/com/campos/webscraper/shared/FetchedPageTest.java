package com.campos.webscraper.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de invariantes do value object FetchedPage.
 *
 * <p>FetchedPage representa a resposta HTTP recebida pelo fetcher.
 * Invariantes testadas:
 * <ul>
 *   <li>URL e fetchedAt são obrigatórios.</li>
 *   <li>isSuccess() retorna true apenas para status 2xx.</li>
 *   <li>isEmpty() detecta corretamente conteúdo ausente.</li>
 *   <li>htmlContent null é normalizado para string vazia.</li>
 * </ul>
 *
 * <p>Ciclo TDD: testes escritos ANTES de FetchedPage existir (fase RED).
 */
@Tag("unit")
@DisplayName("FetchedPage — invariantes de value object")
class FetchedPageTest {

    private static final String VALID_URL      = "https://br.indeed.com/jobs";
    private static final String HTML_CONTENT   = "<html><body><h1>Vagas</h1></body></html>";
    private static final String CONTENT_TYPE   = "text/html; charset=utf-8";
    private static final LocalDateTime NOW     = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Criação válida
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve criar FetchedPage com todos os campos válidos")
    void shouldCreateWithAllValidFields() {
        FetchedPage page = new FetchedPage(VALID_URL, HTML_CONTENT, 200, CONTENT_TYPE, NOW);

        assertThat(page.url()).isEqualTo(VALID_URL);
        assertThat(page.htmlContent()).isEqualTo(HTML_CONTENT);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.contentType()).isEqualTo(CONTENT_TYPE);
        assertThat(page.fetchedAt()).isEqualTo(NOW);
    }

    // -------------------------------------------------------------------------
    // isSuccess()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isSuccess() deve ser true para status 200")
    void shouldBeSuccessFor200() {
        FetchedPage page = new FetchedPage(VALID_URL, HTML_CONTENT, 200, CONTENT_TYPE, NOW);
        assertThat(page.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("isSuccess() deve ser true para status 201")
    void shouldBeSuccessFor201() {
        FetchedPage page = new FetchedPage(VALID_URL, HTML_CONTENT, 201, CONTENT_TYPE, NOW);
        assertThat(page.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("isSuccess() deve ser true para status 299 (limite do range 2xx)")
    void shouldBeSuccessFor299() {
        FetchedPage page = new FetchedPage(VALID_URL, HTML_CONTENT, 299, CONTENT_TYPE, NOW);
        assertThat(page.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("isSuccess() deve ser false para status 400")
    void shouldNotBeSuccessFor400() {
        FetchedPage page = new FetchedPage(VALID_URL, "", 400, CONTENT_TYPE, NOW);
        assertThat(page.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("isSuccess() deve ser false para status 404")
    void shouldNotBeSuccessFor404() {
        FetchedPage page = new FetchedPage(VALID_URL, "", 404, CONTENT_TYPE, NOW);
        assertThat(page.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("isSuccess() deve ser false para status 500")
    void shouldNotBeSuccessFor500() {
        FetchedPage page = new FetchedPage(VALID_URL, "", 500, CONTENT_TYPE, NOW);
        assertThat(page.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("isSuccess() deve ser false para status 301 (redirect — não é 2xx)")
    void shouldNotBeSuccessFor301() {
        FetchedPage page = new FetchedPage(VALID_URL, "", 301, CONTENT_TYPE, NOW);
        assertThat(page.isSuccess()).isFalse();
    }

    // -------------------------------------------------------------------------
    // isEmpty()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isEmpty() deve ser false quando há conteúdo HTML")
    void shouldNotBeEmptyWhenHasHtmlContent() {
        FetchedPage page = new FetchedPage(VALID_URL, HTML_CONTENT, 200, CONTENT_TYPE, NOW);
        assertThat(page.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty() deve ser true quando htmlContent é string vazia")
    void shouldBeEmptyWhenHtmlContentIsEmpty() {
        FetchedPage page = new FetchedPage(VALID_URL, "", 200, CONTENT_TYPE, NOW);
        assertThat(page.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty() deve ser true quando htmlContent é apenas espaços")
    void shouldBeEmptyWhenHtmlContentIsBlank() {
        FetchedPage page = new FetchedPage(VALID_URL, "   ", 200, CONTENT_TYPE, NOW);
        assertThat(page.isEmpty()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Normalização de htmlContent null
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("htmlContent null deve ser normalizado para string vazia")
    void nullHtmlContentShouldBeNormalizedToEmpty() {
        FetchedPage page = new FetchedPage(VALID_URL, null, 200, CONTENT_TYPE, NOW);
        assertThat(page.htmlContent()).isEmpty();
    }

    @Test
    @DisplayName("page com htmlContent null deve ser isEmpty()=true")
    void nullHtmlContentShouldMakePageEmpty() {
        FetchedPage page = new FetchedPage(VALID_URL, null, 200, CONTENT_TYPE, NOW);
        assertThat(page.isEmpty()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Campos obrigatórios
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar NullPointerException quando url for null")
    void shouldThrowWhenUrlIsNull() {
        assertThatThrownBy(() -> new FetchedPage(null, HTML_CONTENT, 200, CONTENT_TYPE, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("url");
    }

    @Test
    @DisplayName("deve lançar NullPointerException quando fetchedAt for null")
    void shouldThrowWhenFetchedAtIsNull() {
        assertThatThrownBy(() -> new FetchedPage(VALID_URL, HTML_CONTENT, 200, CONTENT_TYPE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fetchedAt");
    }
}
