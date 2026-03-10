package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para {@link TargetSiteEntity}.
 *
 * <p>Expõe as queries de domínio necessárias para:
 * <ul>
 *   <li>Resolver um site pelo seu código único ({@link #findBySiteCode}).</li>
 *   <li>Listar sites ativos filtrados por categoria de vaga ({@link #findByEnabledTrueAndJobCategory}).</li>
 *   <li>Listar sites ativos filtrados por status legal ({@link #findByEnabledTrueAndLegalStatus}).</li>
 * </ul>
 *
 * <p>O método herdado {@code findById} e os métodos de escrita ({@code save}, {@code delete})
 * são providos pelo {@code JpaRepository} sem necessidade de declaração explícita.
 */
public interface TargetSiteRepository extends JpaRepository<TargetSiteEntity, Long> {

    /**
     * Busca um site pelo seu código único de negócio.
     *
     * @param siteCode código imutável do site (ex.: {@code "indeed_br"})
     * @return o site encontrado, ou {@code Optional.empty()} se não existir
     */
    Optional<TargetSiteEntity> findBySiteCode(String siteCode);

    /**
     * Lista todos os sites ativos ({@code enabled = true}) de uma categoria específica.
     * Usado pelo scheduler para descobrir quais sites processar em cada ciclo.
     *
     * @param jobCategory categoria de vagas ({@code PRIVATE_SECTOR} ou {@code PUBLIC_CONTEST})
     * @return lista de sites ativos na categoria; vazia se nenhum encontrado
     */
    List<TargetSiteEntity> findByEnabledTrueAndJobCategory(JobCategory jobCategory);

    /**
     * Lista todos os sites ativos ({@code enabled = true}) com determinado status legal.
     * Usado para auditoria de compliance e checklist de onboarding (ADR002).
     *
     * @param legalStatus status legal filtrado (ex.: {@code APPROVED})
     * @return lista de sites ativos com o status informado; vazia se nenhum encontrado
     */
    List<TargetSiteEntity> findByEnabledTrueAndLegalStatus(LegalStatus legalStatus);
}
