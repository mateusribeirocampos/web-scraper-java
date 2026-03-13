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
| ADR007 | TDD e Quality Gates | Proposed |
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
Spring Boot. Rate limit obrigatório por site. Retry, rate limiting, circuit breaker por fonte e
dead-letter já foram implementados; fila assíncrona dedicada ainda permanece planejada.

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
| Contratos de scraper | Implementados para Indeed, DOU e PCI Concursos |
| Modelo JPA de domínio de vagas | Implementado |
| Política de TDD | Mandatória |
| Retry e rate limiting | Implementados |
| Circuit breaker e dead-letter | Implementados |
| Scheduler e trigger manual | Implementados |
| Processamento assíncrono por fila | Planejado |
| Observabilidade | Planejada |
| Governança legal (robots.txt, ToS) | Implementada para o gate de onboarding; expansão planejada |
| Tarefas XP detalhadas | Definidas — 12 iterations |
| Pesquisa open source | Realizada e documentada |
| PCI Concursos | Implementado tecnicamente; produção ainda pendente de checklist legal completo |

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
