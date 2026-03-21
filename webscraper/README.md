# WebScraper

Backend Java 21 + Spring Boot para coleta, normalizacao e disponibilizacao de vagas privadas e
concursos publicos, com prioridade para fontes API-first e fallback controlado para scraping HTML
ou browser somente quando permitido.

## Estado Atual

- Java 21 + Maven
- Persistencia JPA em Postgres
- Scheduler, trigger manual e fila persistida de execucao implementados
- Resilience4j com retry, rate limiting, bulkhead, circuit breaker e dead-letter
- Familias implementadas:
  - Indeed
  - DOU
  - PCI Concursos
  - Greenhouse
  - Gupy
  - Playwright dinamico para sites Type C

## Validacao Manual Oficial

O fluxo manual oficial atual para validar a familia Gupy e a utilidade real dos dados persistidos e:

1. disparar os `CrawlJob`s persistidos pelo endpoint manual;
2. deixar a aplicacao concluir o dispatch/import;
3. consultar apenas vagas recentes com uma intencao de busca reconhecivel pelo usuario.

### Disparo dos jobs

```bash
for id in 15 16 17 18; do
  echo -n "Job $id:"
  curl -s -X POST http://localhost:8080/api/v1/crawl-jobs/$id/execute
  echo ""
done
```

O retorno `{\"jobId\":...,\"status\":\"DISPATCHED\"}` confirma apenas que a execucao foi aceita e
encaminhada. A verificacao funcional acontece na leitura posterior do banco.

### Consulta funcional no banco

```sql
SELECT title, company, seniority, tech_stack_tags, canonical_url
FROM job_postings
WHERE published_at >= CURRENT_DATE - INTERVAL '60 days'
  AND (application_deadline IS NULL OR application_deadline >= CURRENT_DATE)
  AND (
    tech_stack_tags ILIKE '%java%'
    OR tech_stack_tags ILIKE '%spring%'
    OR tech_stack_tags ILIKE '%kotlin%'
    OR title ILIKE '%backend%'
    OR title ILIKE '%software engineer%'
    OR title ILIKE '%desenvolvedor%'
  )
ORDER BY
    CASE seniority
        WHEN 'JUNIOR' THEN 1
        WHEN 'INTERN' THEN 2
        WHEN 'MID'    THEN 3
        WHEN 'SENIOR' THEN 4
        ELSE 5
    END,
    published_at DESC;
```

Essa consulta valida ponta a ponta o caminho real de uso:

- `CrawlJob` persistido
- dispatch da execucao
- import em `job_postings`
- leitura final por uma intencao de busca do usuario

Consulta oficial pela API:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND"
```

Nesse perfil oficial, a API exclui banco de talentos, corta senioridade `SENIOR/LEAD`, bloqueia
cargos de gestao/lideranca e exige sinal real de stack (`Java`, `Spring` ou `Kotlin`) junto com
um sinal de funcao aderente (`backend`, `developer`, `software engineer`, `desenvolvedor`, etc.).

Para investigacao mais ampla da base, sem o perfil estrito:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=UNFILTERED"
```

## Documentacao Relacionada

- `../ADRs-web-scraping-vagas/ADR-Summary-of-WebScraper-ADRs.md`
- `../ADRs-web-scraping-vagas/ADR002-Target-Site-Taxonomy-and-Requirements-for-WebScraper.md`
- `../ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `docs/stories/README.md`
