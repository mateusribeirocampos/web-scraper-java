package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de invariantes do value object ScrapeCommand.
 *
 * <p>ScrapeCommand é imutável (record). As invariantes testadas são:
 * <ul>
 *   <li>Campos obrigatórios: siteCode, targetUrl, extractionMode, jobCategory.</li>
 *   <li>Campos em branco/nulos lançam exceção descritiva.</li>
 *   <li>Igualdade estrutural (comportamento padrão de record).</li>
 * </ul>
 *
 * <p>Ciclo TDD: testes escritos ANTES de ScrapeCommand existir (fase RED).
 */
@Tag("unit")
@DisplayName("ScrapeCommand — invariantes de value object")
class ScrapeCommandTest {

    private static final String VALID_SITE_CODE   = "indeed-br";
    private static final String VALID_URL         = "https://br.indeed.com/jobs?q=java+junior";
    private static final ExtractionMode VALID_MODE = ExtractionMode.API;
    private static final JobCategory VALID_CATEGORY = JobCategory.PRIVATE_SECTOR;

    // -------------------------------------------------------------------------
    // Criação válida
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve criar ScrapeCommand com todos os campos válidos")
    void shouldCreateWithAllValidFields() {
        ScrapeCommand command = new ScrapeCommand(VALID_SITE_CODE, VALID_URL, VALID_MODE, VALID_CATEGORY);

        assertThat(command.siteCode()).isEqualTo(VALID_SITE_CODE);
        assertThat(command.targetUrl()).isEqualTo(VALID_URL);
        assertThat(command.extractionMode()).isEqualTo(VALID_MODE);
        assertThat(command.jobCategory()).isEqualTo(VALID_CATEGORY);
    }

    @Test
    @DisplayName("dois ScrapeCommands com mesmos dados devem ser iguais (record equality)")
    void shouldBeEqualWhenDataIsTheSame() {
        ScrapeCommand command1 = new ScrapeCommand(VALID_SITE_CODE, VALID_URL, VALID_MODE, VALID_CATEGORY);
        ScrapeCommand command2 = new ScrapeCommand(VALID_SITE_CODE, VALID_URL, VALID_MODE, VALID_CATEGORY);

        assertThat(command1).isEqualTo(command2);
        assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
    }

    // -------------------------------------------------------------------------
    // Validação de siteCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar NullPointerException quando siteCode for null")
    void shouldThrowWhenSiteCodeIsNull() {
        assertThatThrownBy(() -> new ScrapeCommand(null, VALID_URL, VALID_MODE, VALID_CATEGORY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("siteCode");
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando siteCode for blank")
    void shouldThrowWhenSiteCodeIsBlank() {
        assertThatThrownBy(() -> new ScrapeCommand("  ", VALID_URL, VALID_MODE, VALID_CATEGORY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("siteCode");
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando siteCode for string vazia")
    void shouldThrowWhenSiteCodeIsEmpty() {
        assertThatThrownBy(() -> new ScrapeCommand("", VALID_URL, VALID_MODE, VALID_CATEGORY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("siteCode");
    }

    // -------------------------------------------------------------------------
    // Validação de targetUrl
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar NullPointerException quando targetUrl for null")
    void shouldThrowWhenTargetUrlIsNull() {
        assertThatThrownBy(() -> new ScrapeCommand(VALID_SITE_CODE, null, VALID_MODE, VALID_CATEGORY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("targetUrl");
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando targetUrl for blank")
    void shouldThrowWhenTargetUrlIsBlank() {
        assertThatThrownBy(() -> new ScrapeCommand(VALID_SITE_CODE, "   ", VALID_MODE, VALID_CATEGORY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetUrl");
    }

    // -------------------------------------------------------------------------
    // Validação de extractionMode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar NullPointerException quando extractionMode for null")
    void shouldThrowWhenExtractionModeIsNull() {
        assertThatThrownBy(() -> new ScrapeCommand(VALID_SITE_CODE, VALID_URL, null, VALID_CATEGORY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("extractionMode");
    }

    // -------------------------------------------------------------------------
    // Validação de jobCategory
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deve lançar NullPointerException quando jobCategory for null")
    void shouldThrowWhenJobCategoryIsNull() {
        assertThatThrownBy(() -> new ScrapeCommand(VALID_SITE_CODE, VALID_URL, VALID_MODE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("jobCategory");
    }
}
