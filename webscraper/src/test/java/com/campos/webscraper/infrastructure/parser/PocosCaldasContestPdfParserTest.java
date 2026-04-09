package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("PocosCaldasContestPdfParser")
class PocosCaldasContestPdfParserTest {

    private final PocosCaldasContestPdfParser parser = new PocosCaldasContestPdfParser();

    @Test
    @DisplayName("should parse official edital number and registration period from municipal PDF text")
    void shouldParseOfficialEditalNumberAndRegistrationPeriodFromMunicipalPdfText() {
        PocosCaldasContestPreviewItem item = parser.parse("""
                EDITAL DE PROCESSO SELETIVO SIMPLIFICADO Nº 001/2025
                PROCESSO SELETIVO PÚBLICO SIMPLIFICADO PARA CONTRATAÇÃO DE PESSOAL POR PRAZO DETERMINADO PARA A PREFEITURA MUNICIPAL DE POÇOS DE CALDAS-MG
                4.1. Período: a partir das 10h do dia 01/09/2025 até às 16h do dia 09/09/2025.
                5.1. Período: a partir das 10h do dia 01/09/2025 até às 16h do dia 09/09/2025.
                """, "https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf");

        assertThat(item.contestTitle()).isEqualTo("Edital de Processo Seletivo Simplificado nº 001/2025");
        assertThat(item.organizer()).isEqualTo("Prefeitura Municipal de Poços de Caldas");
        assertThat(item.contestNumber()).isEqualTo("001/2025");
        assertThat(item.editalYear()).isEqualTo(2025);
        assertThat(item.publishedAt()).isNull();
        assertThat(item.registrationStartDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(item.registrationEndDate()).isEqualTo(LocalDate.of(2025, 9, 9));
        assertThat(item.examDate()).isNull();
    }

    @Test
    @DisplayName("should accept N degree symbol and hhmm registration windows from pdf text extraction")
    void shouldAcceptNDegreeSymbolAndHhmmRegistrationWindowsFromPdfTextExtraction() {
        PocosCaldasContestPreviewItem item = parser.parse("""
                EDITAL DE PROCESSO SELETIVO SIMPLIFICADO N° 001/2025
                PROCESSO SELETIVO PÚBLICO SIMPLIFICADO PARA CONTRATAÇÃO DE PESSOAL
                4.1. Período: a partir das 10h00 do dia 01/09/2025 até às 16h00 do dia 09/09/2025.
                """, "https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf");

        assertThat(item.contestNumber()).isEqualTo("001/2025");
        assertThat(item.registrationStartDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(item.registrationEndDate()).isEqualTo(LocalDate.of(2025, 9, 9));
    }

    @Test
    @DisplayName("should select canonical edital from generic download link using URL keywords")
    void shouldSelectCanonicalEditalFromGenericDownloadLinkUsingUrlKeywords() {
        String editalUrl = parser.selectCanonicalEditalUrl("""
                <html><body>
                  <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf">
                    <img src="/download.svg" alt="download">
                  </a>
                </body></html>
                """, "https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609");

        assertThat(editalUrl)
                .isEqualTo("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf");
    }

    @Test
    @DisplayName("should prefer higher edital number when multiple same-year primary PDFs exist")
    void shouldPreferHigherEditalNumberWhenMultipleSameYearPrimaryPdfsExist() {
        String editalUrl = parser.selectCanonicalEditalUrl("""
                <html><body>
                  <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/01/EDITAL-DE-PROCESSO-SELETIVO-001-2026.pdf">Edital 001/2026</a>
                  <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/02/EDITAL-DE-PROCESSO-SELETIVO-002-2026.pdf">Edital 002/2026</a>
                </body></html>
                """, "https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609");

        assertThat(editalUrl)
                .isEqualTo("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/02/EDITAL-DE-PROCESSO-SELETIVO-002-2026.pdf");
    }

    @Test
    @DisplayName("should prefer newer edital year before older years")
    void shouldPreferNewerEditalYearBeforeOlderYears() {
        String editalUrl = parser.selectCanonicalEditalUrl("""
                <html><body>
                  <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf">Edital 001/2025</a>
                  <a href="https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/01/EDITAL-DE-PROCESSO-SELETIVO-001-2026.pdf">Edital 001/2026</a>
                </body></html>
                """, "https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609");

        assertThat(editalUrl)
                .isEqualTo("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2026/01/EDITAL-DE-PROCESSO-SELETIVO-001-2026.pdf");
    }
}
