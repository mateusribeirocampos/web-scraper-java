package com.campos.webscraper.infrastructure.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("ExtremaConcursosParser")
class ExtremaConcursosParserTest {

    private final ExtremaConcursosParser parser = new ExtremaConcursosParser();

    @Test
    @DisplayName("should extract contest detail urls from official Extrema education listing")
    void shouldExtractContestDetailUrlsFromOfficialExtremaEducationListing() {
        List<String> urls = parser.parseListingUrls("""
                <html><body>
                  <a class="list-item__link" href="/secretarias/educacao/processo-seletivo-publico-simplificado-para-contratacao-de-cargosfuncoes-publicas-para-o-quadro-da-educacao-de-extrema">
                    Processo seletivo público simplificado para contratação de cargos/funções públicas para o quadro da Educação de Extrema
                  </a>
                  <a class="list-item__link" href="/secretarias/educacao/politica-de-educacao-em-tempo-integral">
                    Política de Educação em Tempo Integral
                  </a>
                  <a class="list-item__link" href="/secretarias/educacao/edital-de-selecao-de-formadores-do-leei">
                    Edital de Seleção de Formadores do Leei
                  </a>
                </body></html>
                """, "https://www.extrema.mg.gov.br/secretarias/educacao");

        assertThat(urls).containsExactly(
                "https://www.extrema.mg.gov.br/secretarias/educacao/processo-seletivo-publico-simplificado-para-contratacao-de-cargosfuncoes-publicas-para-o-quadro-da-educacao-de-extrema",
                "https://www.extrema.mg.gov.br/secretarias/educacao/edital-de-selecao-de-formadores-do-leei"
        );
    }

    @Test
    @DisplayName("should choose canonical edital and ignore follow-up attachments on detail page")
    void shouldChooseCanonicalEditalAndIgnoreFollowUpAttachmentsOnDetailPage() {
        ExtremaContestPreviewItem item = parser.parseDetail("""
                <html>
                  <head>
                    <meta property="article:published_time" content="2025-07-17T20:08:37Z" />
                  </head>
                  <body>
                    <main class="page-secretarias__grid-item conteudo">
                      <h1 class="page-secretarias__titulo">Processo seletivo público simplificado para contratação de cargos/funções públicas para o quadro da Educação de Extrema</h1>
                      <p><a href="https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_17072025170837.pdf">Edital nº003/2025 sobre o processo seletivo público simplificado</a></p>
                      <p><a href="https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_12082025091208.pdf">Retificação do Edital nº 003/2025</a></p>
                      <p><a href="https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_07012026085733.pdf">2ª Convocação do Processo Seletivo Simplificado</a></p>
                    </main>
                  </body>
                </html>
                """, "https://www.extrema.mg.gov.br/secretarias/educacao/processo-seletivo-publico");

        assertThat(item).isNotNull();
        assertThat(item.contestNumber()).isEqualTo("003/2025");
        assertThat(item.editalYear()).isEqualTo(2025);
        assertThat(item.editalUrl()).isEqualTo("https://ecrie.com.br/sistema/conteudos/arquivo/a_240_0_1_17072025170837.pdf");
        assertThat(item.publishedAt()).isEqualTo(java.time.LocalDate.of(2025, 7, 17));
        assertThat(item.attachments()).hasSize(3);
    }
}
