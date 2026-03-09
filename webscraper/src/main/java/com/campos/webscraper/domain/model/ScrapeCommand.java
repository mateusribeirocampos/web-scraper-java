package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;

import java.util.Objects;

/**
 * Comando imutável que instrui uma {@code JobScraperStrategy} a executar
 * uma operação de extração em um site-alvo.
 *
 * <p>É um value object (record Java) — dois {@code ScrapeCommand} com os
 * mesmos dados são considerados iguais ({@code equals}/{@code hashCode}
 * delegados automaticamente ao compilador).
 *
 * <p>Invariantes validadas no construtor compacto:
 * <ul>
 *   <li>{@code siteCode} não pode ser null nem blank.</li>
 *   <li>{@code targetUrl} não pode ser null nem blank.</li>
 *   <li>{@code extractionMode} não pode ser null.</li>
 *   <li>{@code jobCategory} não pode ser null.</li>
 * </ul>
 *
 * <p>Exemplo de uso:
 * <pre>{@code
 * ScrapeCommand command = new ScrapeCommand(
 *     "indeed-br",
 *     "https://br.indeed.com/jobs?q=java+junior",
 *     ExtractionMode.API,
 *     JobCategory.PRIVATE_SECTOR
 * );
 * }</pre>
 *
 * @param siteCode       identificador único do site-alvo (ex.: "indeed-br", "pci-concursos")
 * @param targetUrl      URL de entrada para a extração
 * @param extractionMode modo de extração a ser utilizado pela strategy
 * @param jobCategory    categoria da vaga (setor privado ou concurso público)
 */
public record ScrapeCommand(
        String siteCode,
        String targetUrl,
        ExtractionMode extractionMode,
        JobCategory jobCategory
) {

    /**
     * Construtor compacto com validação de invariantes.
     * Executado automaticamente pelo compilador antes de qualquer instanciação.
     */
    public ScrapeCommand {
        Objects.requireNonNull(siteCode, "siteCode must not be null");
        Objects.requireNonNull(targetUrl, "targetUrl must not be null");
        Objects.requireNonNull(extractionMode, "extractionMode must not be null");
        Objects.requireNonNull(jobCategory, "jobCategory must not be null");

        if (siteCode.isBlank()) {
            throw new IllegalArgumentException("siteCode must not be blank");
        }
        if (targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl must not be blank");
        }
    }
}
