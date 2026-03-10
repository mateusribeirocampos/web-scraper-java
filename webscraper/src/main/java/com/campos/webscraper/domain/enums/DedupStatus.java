package com.campos.webscraper.domain.enums;

/**
 * Resultado da avaliação de deduplicação de uma vaga ou concurso.
 *
 * <p>Determinado pelo {@code FingerprintCalculator} ao comparar o {@code fingerprintHash}
 * calculado com os registros já persistidos na base de dados.
 *
 * <p>Controla o comportamento do pipeline de persistência:
 * {@code NEW} → inserir, {@code DUPLICATE} → descartar, {@code UPDATED} → atualizar.
 */
public enum DedupStatus {

    /**
     * Fingerprint nunca visto antes — vaga nova, deve ser inserida na base.
     * Caso mais frequente em execuções normais do crawler.
     */
    NEW,

    /**
     * Fingerprint idêntico já existe na base — vaga já foi coletada anteriormente.
     * O registro deve ser descartado sem gerar insert ou update.
     */
    DUPLICATE,

    /**
     * Mesma vaga (mesmo {@code siteCode} + {@code externalId}), mas algum campo
     * mutável foi alterado na fonte (ex.: salário, título, status).
     * O registro existente deve ser atualizado, preservando o histórico.
     */
    UPDATED
}
