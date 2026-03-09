package com.campos.webscraper.domain.enums;

/**
 * Estado atual de um concurso público ao longo do seu ciclo de vida.
 *
 * <p>Utilizado em {@code PublicContestPostingEntity.contestStatus} para
 * refletir a fase em que o processo seletivo se encontra.
 *
 * <p>A ordem da declaração representa a progressão temporal natural de um
 * concurso público.
 */
public enum ContestStatus {

    /**
     * Inscrições abertas — concurso disponível para candidatos se inscreverem.
     * Status de maior interesse para o usuário da plataforma.
     */
    OPEN,

    /**
     * Inscrições encerradas — prazo de inscrição expirado, aguardando próxima etapa.
     */
    REGISTRATION_CLOSED,

    /**
     * Provas agendadas — data de realização do exame confirmada no edital.
     */
    EXAM_SCHEDULED,

    /**
     * Resultado publicado — gabarito definitivo ou lista de aprovados disponíveis.
     */
    RESULT_PUBLISHED,

    /**
     * Concurso cancelado — suspenso por decisão administrativa ou judicial.
     */
    CANCELLED
}
