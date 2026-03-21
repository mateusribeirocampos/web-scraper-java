# Resumo Executivo dos ADRs — WebScraper de Vagas Java Júnior / Spring Boot

## Visão Geral

Esta plataforma coleta, normaliza e disponibiliza vagas de emprego (setor privado) e concursos
públicos brasileiros, filtradas especificamente para a stack Java Júnior / Spring Boot.

**Princípios fundamentais:**
1. **API-first obrigatório** — API oficial sempre tem prioridade absoluta sobre scraping HTML.
2. **Java como única linguagem de produção** — alinhamento com stack de aprendizagem e carreira.
3. **TDD mandatório** — toda feature começa com teste falhando, nunca com implementação.
4. **Conformidade legal** — robots.txt, ToS e status de aprovação registrados antes de qualquer scraping.
5. **Dados disponibilizados por data** — `publishedAt` é campo obrigatório em toda entidade de vaga.

---

## Mandato TDD

**Todas as features deste projeto devem ser implementadas com TDD como regra não-negociável.**

O ciclo obrigatório em toda story:

1. Escrever o teste falhando primeiro.
2. Implementar o mínimo de código para fazer o teste passar.
3. Refatorar com todos os testes verdes.
4. Promover a feature somente quando unit, integration e contract checks estiverem passando.

Nenhum scraper, parser, mapper, adaptador de persistência, scheduler, política de retry,
rate-limiter ou regra de deduplicação pode ser considerado completo sem testes escritos antes
da implementação.

---

## Índice de ADRs

| ADR | Título | Status |
|---|---|---|
| ADR001 | Direção Arquitetural e de Tecnologia | Accepted |
| ADR002 | Taxonomia de Sites, Análise Legal e Requisitos | Accepted |
| ADR003 | Avaliação Stack Java vs Python e Open Source | Accepted |
| ADR004 | Arquitetura de Extração e Strategy/Factory | Accepted |
| ADR005 | Modelo de Domínio JPA e Persistência | Accepted |
| ADR006 | Resiliência, Rate Limiting e Processamento Assíncrono | Proposed |
| ADR007 | TDD e Quality Gates | Accepted |
| ADR008 | Observabilidade, Segurança e Governança | Proposed |
| ADR009 | Plano de Entrega XP com Tarefas Detalhadas | Accepted |
| ADR010 | Pesquisa Open Source e Projetos GitHub | Accepted |

---

## Destaques Principais

**Stack de produção:** Java 21 + Spring Boot 3.x — linguagem única, alinhamento com portfólio.

**Estratégia de aquisição de dados (por prioridade):**
1. API oficial (Indeed MCP Connector, DOU API gov) — fonte primária, menor risco, zero scraping.
2. APIs/boards públicos de ATS (Greenhouse, Lever, Ashby) — expansão segura para PMEs e startups.
3. Páginas de carreira próprias com `JobPosting` estruturado e `robots.txt` aprovado.
4. Scraping HTML estático via jsoup — para fontes classificadas como permitidas após revisão legal.
5. Playwright for Java — fallback dinâmico exclusivo para sites Tipo C comprovados.

**Fontes de dados brasileiras analisadas:**

| Fonte | Tipo | Estratégia |
|---|---|---|
| Indeed Brasil | API oficial | MCP Connector — **primeira integração a implementar** |
| DOU (in.gov.br) | API pública gov | API gov — concursos federais |
| Greenhouse / Lever / Ashby | API/board público | Expansão segura para empresas pequenas e médias |
| PCI Concursos | HTML estático (Tipo A) | jsoup após revisão de robots.txt |
| LinkedIn | Tipo D, ToS proíbe | Excluído da fase 1 |
| Catho / Glassdoor | ToS proíbe | Excluídos da fase 1 |
| Codante.io Jobs API | API pública | Apenas dev/test |

**Modelo de domínio:** `JobPostingEntity` (vagas privadas) e `PublicContestPostingEntity`
(concursos públicos), ambos com campo mandatório `publishedAt: LocalDate` para filtragem
e ordenação cronológica.

**Resiliência:** Resilience4j com Retry, RateLimiter, Bulkhead e CircuitBreaker integrados ao
Spring Boot. Rate limit obrigatório por site. Retry, rate limiting, circuit breaker por fonte,
dead-letter e fila persistida de execução já foram implementados.

**Teste de aceite do usuário:** além do TDD e dos testes automatizados, toda nova família de fonte
deve fechar com um cenário manual reproduzível de execução e leitura dos resultados, usando uma
intenção de busca reconhecível pelo usuário final, como `desenvolvedor de software em java spring boot`
ou `concurso analista de ti`.

**Estilo arquitetural:** Backend em camadas com contratos explícitos de extrator, strategies por
site, factory de resolução, scheduler/orquestrador e persistência separada da lógica de scraping.

**Modelo de entrega:** XP/TDD-first, com stories por iteration, red-green-refactor mandatório,
CI quality gates e rollout controlado por família de scraper.

---

## Snapshot de Conformidade Técnica

| Item | Status Atual |
|---|---|
| Linguagem de produção | Java 21 — definido e confirmado |
| API-first como prioridade | Definido |
| Contratos de scraper | Implementados para Indeed, DOU, PCI Concursos, Greenhouse, Gupy e Playwright dinâmico |
| Modelo JPA de domínio de vagas | Implementado |
| Política de TDD | Mandatória |
| Retry e rate limiting | Implementados |
| Circuit breaker e dead-letter | Implementados |
| Scheduler e trigger manual | Implementados |
| Processamento assíncrono por fila | Implementado com fila persistida |
| Observabilidade | Planejada |
| Governança legal (robots.txt, ToS) | Implementada para o gate de onboarding; expansão planejada |
| Tarefas XP detalhadas | Definidas — 12 iterations |
| Pesquisa open source | Realizada e documentada |
| PCI Concursos | Implementado tecnicamente; produção ainda pendente de checklist legal completo |
| Greenhouse Bitso | Onboarding, client, normalizer, strategy e persistência ponta a ponta implementados |
| Gupy | Client, normalizer, strategy, import use case e seed operacional implementados |

---

## Uso Atual do Projeto Para Teste do Usuário

No estado atual, o usuário já consegue validar o comportamento do scraper no sistema rodando,
mesmo sem busca livre exposta como input público. O procedimento oficial de aceite manual para a
família Gupy passou a ser:

1. disparar manualmente os `CrawlJob`s persistidos da família validada
2. aguardar o despacho e a persistência das execuções
3. consultar diretamente `job_postings` com recência obrigatória e um filtro representativo da intenção do usuário
4. comparar os resultados retornados com a busca desejada

Consultas de referência documentadas:

- `desenvolvedor de software em java spring boot`
- `concurso analista de ti`

Fluxo manual atualmente validado:

```bash
for id in 15 16 17 18; do
  echo -n "Job $id:"
  curl -s -X POST http://localhost:8080/api/v1/crawl-jobs/$id/execute
  echo ""
done
```

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

Hoje essa intenção ainda precisa estar refletida na configuração do job/fonte. A busca livre
customizada continua como evolução funcional futura, mas esse fluxo já valida a aplicação de ponta
a ponta com uma pesquisa reconhecível pelo usuário.

Para consumo via API, o contrato oficial de listagem de vagas recentes passa a ser:

```text
GET /api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND
```

`since` continua suportado e sobrescreve `daysBack` quando informado explicitamente.
O perfil oficial também exclui banco de talentos, reduz senioridade alta por padrão, bloqueia
cargos de gestão/liderança e exige sinal real de stack junto com sinal de função aderente para
evitar falso positivo por títulos genéricos.

Também existe um perfil intermediário:

```text
GET /api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_BACKEND_BALANCED
```

Ele preserva o filtro de recência, bloqueia banco de talentos e liderança, mas abre mão do corte
estrito de senioridade e do sinal obrigatório de função aderente.

Também existe um perfil pragmático:

```text
GET /api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_STACK_PRAGMATIC
```

Ele mantém recência, exclui banco de talentos e exige stack aderente, mas aceita resultados mais
amplos para aumentar o volume útil quando a base recente estiver escassa.

---

## Mapa de Dependências entre ADRs

```
ADR001 (Arquitetura)
  ├── ADR002 (Sites + Legal)
  │     └── ADR010 (Open Source)
  ├── ADR003 (Stack + Open Source)
  │     └── ADR010 (Open Source)
  ├── ADR004 (Extração)
  │     └── ADR005 (Domínio)
  ├── ADR005 (Domínio + Persistência)
  ├── ADR006 (Resiliência)
  ├── ADR007 (TDD + Quality Gates)
  ├── ADR008 (Observabilidade)
  └── ADR009 (Plano XP)
            └── (referencia todos os ADRs acima)
```

---

## Convenção de Nomenclatura

Arquivos ADR seguem:

`ADR[3-dígitos]-[título-kebab-case].md`
