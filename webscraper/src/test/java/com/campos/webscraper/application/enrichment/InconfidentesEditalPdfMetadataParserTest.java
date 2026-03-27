package com.campos.webscraper.application.enrichment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("InconfidentesEditalPdfMetadataParser")
class InconfidentesEditalPdfMetadataParserTest {

    @Test
    @DisplayName("should extract registration window education exam date and position from edital text")
    void shouldExtractRegistrationWindowEducationExamDateAndPositionFromEditalText() {
        InconfidentesEditalPdfMetadataParser parser = new InconfidentesEditalPdfMetadataParser();

        String pdfText = """
                PREFEITURA MUNICIPAL DE INCONFIDENTES
                EDITAL 021/2026
                Cargo: Analista de Sistemas
                Escolaridade: ensino superior completo em Ciencia da Computacao, Sistemas de Informacao
                Inscricoes: de 10/04/2026 a 20/04/2026
                A prova objetiva sera realizada em 30/05/2026.
                """;

        InconfidentesEditalPdfMetadata metadata = parser.parse(pdfText);

        assertThat(metadata.positionTitle()).isEqualTo("Analista de Sistemas");
        assertThat(metadata.educationLevel()).isEqualTo("SUPERIOR");
        assertThat(metadata.formationRequirements()).contains("ensino superior");
        assertThat(metadata.registrationStartDate()).isEqualTo(java.time.LocalDate.parse("2026-04-10"));
        assertThat(metadata.registrationEndDate()).isEqualTo(java.time.LocalDate.parse("2026-04-20"));
        assertThat(metadata.examDate()).isEqualTo(java.time.LocalDate.parse("2026-05-30"));
    }

    @Test
    @DisplayName("should preserve missing metadata when edital text does not expose the signals")
    void shouldPreserveMissingMetadataWhenEditalTextDoesNotExposeTheSignals() {
        InconfidentesEditalPdfMetadataParser parser = new InconfidentesEditalPdfMetadataParser();

        InconfidentesEditalPdfMetadata metadata = parser.parse("""
                PREFEITURA MUNICIPAL DE INCONFIDENTES
                EDITAL EXTRAORDINARIO
                Documento de referencia sem cronograma estruturado.
                """);

        assertThat(metadata.positionTitle()).isNull();
        assertThat(metadata.educationLevel()).isNull();
        assertThat(metadata.formationRequirements()).isNull();
        assertThat(metadata.registrationStartDate()).isNull();
        assertThat(metadata.registrationEndDate()).isNull();
        assertThat(metadata.examDate()).isNull();
    }

    @Test
    @DisplayName("should preserve aggregate html title when edital exposes multiple cargos")
    void shouldPreserveAggregateHtmlTitleWhenEditalExposesMultipleCargos() {
        InconfidentesEditalPdfMetadataParser parser = new InconfidentesEditalPdfMetadataParser();

        InconfidentesEditalPdfMetadata metadata = parser.parse("""
                PREFEITURA MUNICIPAL DE INCONFIDENTES
                EDITAL 021/2026
                Cargo: Analista de Sistemas
                Cargo: Tecnico em Informatica
                Escolaridade: ensino superior completo
                """);

        assertThat(metadata.positionTitle()).isNull();
        assertThat(metadata.educationLevel()).isEqualTo("SUPERIOR");
    }

    @Test
    @DisplayName("should keep education level unknown when the pdf mentions a tecnico cargo but no schooling section")
    void shouldKeepEducationLevelUnknownWhenThePdfMentionsATecnicoCargoButNoSchoolingSection() {
        InconfidentesEditalPdfMetadataParser parser = new InconfidentesEditalPdfMetadataParser();

        InconfidentesEditalPdfMetadata metadata = parser.parse("""
                PREFEITURA MUNICIPAL DE INCONFIDENTES
                EDITAL 021/2026
                Cargo: Tecnico em Informatica
                Documento sem secao estruturada de escolaridade.
                """);

        assertThat(metadata.positionTitle()).isEqualTo("Tecnico em Informatica");
        assertThat(metadata.educationLevel()).isNull();
        assertThat(metadata.formationRequirements()).isNull();
    }
}
