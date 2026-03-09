package com.campos.webscraper.domain.enums;

/**
 * Regime contratual de uma vaga de emprego no setor privado.
 *
 * <p>Utilizado em {@code JobPostingEntity.contractType} para normalizar
 * a forma como diferentes sites descrevem o vínculo empregatício.
 */
public enum JobContractType {

    /** Consolidação das Leis do Trabalho — regime formal brasileiro com FGTS, 13°, etc. */
    CLT,

    /** Pessoa Jurídica — prestação de serviços via CNPJ, sem vínculo empregatício. */
    PJ,

    /** Estágio — contrato regido pela Lei 11.788/2008 (Lei do Estágio). */
    INTERNSHIP,

    /** Trabalho freelancer / projeto pontual — sem vínculo contínuo. */
    FREELANCE,

    /** Contrato temporário — regido pela Lei 6.019/1974. */
    TEMPORARY
}
