# ADR 009 — Plano de Entrega XP e Tarefas Detalhadas para o WebScraper de Vagas

## Title

Plano de implementação orientado a XP (Extreme Programming) com tarefas granulares para a
plataforma WebScraper de vagas Java Júnior / Spring Boot e concursos públicos brasileiros.

## Status

Accepted

## Date

2026-03-09

## Context

O projeto precisa de um roadmap de implementação prático que traduza a arquitetura em
deliverables granulares. O plano torna o TDD explícito em cada feature, identifica as primeiras
integrações API-first a serem implementadas (Indeed MCP e DOU API) e progride de forma
incremental até os scrapers HTML estáticos e o fallback dinâmico.

Após a conclusão das primeiras fatias Indeed e DOU, o roadmap passa a explicitar também a
expansão para vagas de empresas pequenas e médias por meio de providers ATS públicos
(Greenhouse/Lever/Ashby) e páginas de carreira próprias com onboarding legal aprovado. O projeto
não deve ficar limitado a concursos nem depender cedo de boards generalistas com risco jurídico
mais alto.

---

## XP Delivery Rules

- Stories pequenas com valor de negócio visível.
- Red → Green → Refactor mandatório em toda story.
- Pair-review ou code review estruturado para regras de parser.
- Integração contínua obrigatória antes de merge.
- Refatoração faz parte do critério de pronto, não é cleanup opcional.
- Toda nova família de fonte deve fechar também um teste manual de aceite orientado ao usuário.

### Manual User Acceptance Rule

Além dos testes automatizados, cada nova integração de fonte deve prever um cenário manual
reproduzível de verificação funcional no sistema rodando.

Formato mínimo do cenário:

1. Disparar o job manualmente.
2. Confirmar que a execução concluiu com sucesso.
3. Consultar os endpoints de leitura do sistema.
4. Verificar se os itens retornados correspondem à intenção de busca do usuário.

Consultas de referência:

- vagas privadas: `desenvolvedor de software em java spring boot`
- concursos: `concurso analista de ti` ou `concurso desenvolvedor java`

Observação importante:

- enquanto a plataforma ainda não expuser busca customizada como input público, esse teste manual
  pode ser executado com um `CrawlJob` pré-configurado ou uma URL/endpoint de origem que já
  represente a pesquisa desejada;
- quando a busca customizada virar feature do produto, esse mesmo cenário passa a ser requisito
  explícito de aceite end-to-end.

### Current Project Usage For User Validation

No estado atual do projeto em 2026-03-13, o uso manual para validar scraping funciona assim:

1. Configurar um `CrawlJob` cuja fonte represente a busca desejada.
2. Disparar:
   - `POST /api/v1/crawl-jobs/{jobId}/execute`
3. Acompanhar a execução persistida.
4. Consultar a saída persistida:
   - `GET /api/v1/job-postings?since=YYYY-MM-DD&category=PRIVATE_SECTOR&seniority=...`
   - `GET /api/v1/public-contests?...`
5. Confirmar se os resultados correspondem à intenção do usuário.

Exemplos de intenção já adotados como referência:

- `desenvolvedor de software em java spring boot`
- `concurso analista de ti`

Portanto, o projeto já suporta validação funcional real pelo usuário, mas ainda não expõe busca
livre customizada como parâmetro público de produto. Isso continua como evolução futura.

---

## Detailed Tasks by Iteration

### Iteration 1 — Foundation e esqueleto do projeto

#### Story 1.1 — Bootstrap do projeto Spring Boot
- Criar projeto base Spring Boot com Maven (módulos ou estrutura de pacotes).
- Adicionar dependências: Web, Validation, Data JPA, Actuator, Test, Testcontainers, jsoup,
  OkHttp, Resilience4j.
- Configurar `application.properties` base.
- Configurar Flyway para migrations.
- **TDD:** iniciar com context-load test e configuration validation test.

#### Story 1.2 — Criar enums e value objects do domínio de vagas
- Definir `SiteType`, `ExtractionMode`, `JobCategory`, `LegalStatus`.
- Definir `JobContractType`, `SeniorityLevel`, `GovernmentLevel`, `EducationLevel`.
- Definir `ContestStatus`, `DedupStatus`, `CrawlExecutionStatus`.
- Definir `ScrapeCommand`, `ScrapeResult<T>`, `FetchedPage`, `FetchRequest`.
- **TDD:** testes de invariantes de value objects antes de qualquer código de produção.

#### Story 1.3 — Criar arquitetura de pacotes
- `domain` (entidades, value objects, enums, interfaces de repositório)
- `application` (use cases, orchestrators, normalizers)
- `infrastructure` (JPA entities, repositories, fetchers, API clients)
- `interfaces` (REST controllers, schedulers)
- `shared` (utils, fingerprint, exceptions)
- **TDD:** smoke test de wiring de beans para o primeiro use case.

---

### Iteration 2 — Modelo de persistência

#### Story 2.1 — Implementar `TargetSiteEntity`
- Criar entidade e repository.
- Adicionar migration V001.
- Adicionar campos `jobCategory`, `legalStatus`, `selectorBundleVersion`.
- **TDD:** repository integration test com Testcontainers primeiro.

#### Story 2.2 — Implementar `CrawlJobEntity` e `CrawlExecutionEntity`
- Criar entidades, relacionamentos, índices.
- Adicionar migrations V002.
- **TDD:** testes de integração para persistência de relacionamentos e transições de status primeiro.

#### Story 2.3 — Implementar `JobPostingEntity` e campos de deduplicação
- Criar entidade com `publishedAt`, `fingerprintHash`, `contractType`, `seniority`, `techStackTags`.
- Adicionar migration V004.
- Implementar `JobPostingFingerprintCalculator`.
- **TDD:** entity mapping test + repository test com Testcontainers + dedup rule test primeiro.

#### Story 2.4 — Implementar `PublicContestPostingEntity`
- Criar entidade com `contestName`, `organizer`, `registrationEndDate`, `numberOfVacancies`,
  `contestStatus`, etc.
- Adicionar migration V005.
- Implementar `ContestPostingFingerprintCalculator`.
- **TDD:** entity mapping test + repository test + test de consulta por `registrationEndDate` primeiro.

---

### Iteration 3 — Contratos de Strategy e Factory

#### Story 3.1 — Criar contrato `JobScraperStrategy`
- Adicionar interface e tipos de resultado.
- **TDD:** definir contract tests primeiro.

#### Story 3.2 — Criar `JobScraperFactory`
- Resolver strategy por metadados explícitos do site.
- Lançar `UnsupportedSiteException` descritiva quando sem suporte.
- **TDD:** testes de resolução falhando primeiro (para cada strategy planejada).

#### Story 3.3 — Criar abstração `JobFetcher`
- Contrato de interface `JobFetcher` com implementação `HttpJobFetcher` (OkHttp).
- **TDD:** testes com transporte mockado (`WireMock` ou servidor HTTP equivalente) primeiro.

---

### Iteration 4 — Primeira integração API-first: Indeed MCP Connector

Esta é a integração de maior valor e menor risco técnico. Deve ser a **primeira source implementada**.

#### Story 4.1 — Implementar `IndeedApiClient`
- Client HTTP para o Indeed MCP connector.
- Serialização/deserialização da resposta JSON.
- **TDD:** testes com transporte mockado/fixture de resposta JSON primeiro.

Exemplo de fixture de resposta JSON do Indeed MCP:
```json
{
  "jobId": "5-cmh1-0-1jj9snbbvr8er800-358c3bd3a6b73ba5",
  "title": "Java Backend Developer | Jr (Remote)",
  "company": "Invillia",
  "location": "Remoto",
  "postedAt": "2026-03-05",
  "applyUrl": "https://to.indeed.com/aas2tpyk2v6d"
}
```

#### Story 4.2 — Implementar `IndeedJobNormalizer`
- Mapear `IndeedApiResponse` → `JobPosting` com `publishedAt`, `seniority = JUNIOR`,
  `techStackTags = "Java,Spring Boot"`.
- **TDD:** testes de normalização de campos primeiro.

#### Story 4.3 — Implementar `IndeedApiJobScraperStrategy`
- Integrar client + normalizer.
- **TDD:** testes de extração via fixture de resposta API primeiro.

#### Story 4.4 — Persistir vagas do Indeed
- Executar fluxo completo: command → strategy → normalize → persist.
- **TDD:** integration test para fatia completa primeiro.

---

### Iteration 5 — Segunda integração API-first: DOU API (Concursos Federais)

#### Story 5.1 — Implementar `DouApiClient`
- Client REST para API do Diário Oficial da União (in.gov.br/dados-abertos).
- Filtrar por palavras-chave: "Analista de TI", "Desenvolvedor", "Tecnologia da Informação".
- **TDD:** testes com transporte mockado + fixture JSON do DOU primeiro.

#### Story 5.2 — Implementar `DouContestNormalizer`
- Mapear resposta DOU → `PublicContestPosting` com `governmentLevel = FEDERAL`,
  `publishedAt`, `editalUrl`, `organizer`.
- **TDD:** testes de normalização primeiro.

#### Story 5.3 — Implementar `DouApiContestScraperStrategy`
- Integrar client + normalizer.
- **TDD:** testes de extração via fixture DOU primeiro.

#### Story 5.4 — Persistir concursos do DOU
- Executar fluxo completo para concursos federais.
- **TDD:** integration test para fatia completa de concursos primeiro.

---

### Iteration 6 — Agendamento e execução manual

#### Story 6.1 — Trigger por agendador (scheduler)
- Execução cron para jobs habilitados.
- **TDD:** testes de trigger do scheduler primeiro.

#### Story 6.2 — Endpoint de execução manual
- `POST /api/v1/crawl-jobs/{jobId}/execute`
- **TDD:** controller/use-case tests primeiro.

#### Story 6.3 — Log de execução
- Persistir status started/succeeded/failed e contadores.
- **TDD:** testes de ciclo de vida de status primeiro.

#### Story 6.4 — Endpoint de listagem de vagas por data
- `GET /api/v1/job-postings?since=2026-03-01&category=PRIVATE_SECTOR&seniority=JUNIOR`
- `GET /api/v1/public-contests?status=OPEN&orderBy=registrationEndDate`
- **TDD:** testes de endpoint com consulta por `publishedAt` primeiro.

---

### Iteration 7 — Baseline de resiliência

#### Story 7.1 — Política de retry
- Adicionar Resilience4j retry em torno do fetcher.
- **TDD:** testes de exceção retryable vs non-retryable primeiro.

#### Story 7.2 — Rate limiting
- Adicionar perfil de rate-limiter por site.
- Nenhum site pode ter perfil "sem limite".
- **TDD:** testes de denial e permit primeiro.

#### Story 7.3 — Circuit breaker e dead-letter
- Abrir circuito após threshold de falha sustentada.
- Rotear falhas esgotadas para dead-letter.
- **TDD:** testes de open-state e roteamento primeiro.

---

### Iteration 8 — Primeiro scraper HTML estático: PCI Concursos

#### Story 8.1 — Capturar fixture HTML do PCI Concursos
- Salvar HTML representativo da página de listagem de concursos de TI.
- Salvar output esperado do parse (campos normalizados).
- Documentar seletores CSS identificados via inspeção + referência do projeto open source
  `luiseduardobr1/PCIConcursos` (ADR010).
- **TDD:** fixture parser test vem antes da implementação.

#### Story 8.2 — Implementar `SelectorBundle` para PCI Concursos
- Mapear campos: `contestName`, `organizer`, `numberOfVacancies`, `salaryRange`,
  `registrationDeadline`, `detailUrl`.
- Versionar bundle como `pci_concursos_v1`.
- **TDD:** testes de mapeamento de seletores primeiro.

#### Story 8.3 — Implementar `PciConcursosScraperStrategy`
- Parsing HTML via jsoup com seletores do bundle.
- Paginação: seguir links de próxima página até fim.
- **TDD:** testes de extração com fixture HTML primeiro.

#### Story 8.4 — Verificar robots.txt e ToS do PCI Concursos antes de ativar em produção
- Documentar resultado da revisão no campo `legalStatus` do `TargetSiteEntity`.
- Preencher checklist de onboarding (ADR002, Seção 3).
- **Bloqueante:** scraper não é ativado sem checklist completo.

---

### Iteration 9 — Expansão do setor privado para PMEs via ATS público (Greenhouse first)

#### Story 9.1 — Onboarding legal e seleção do primeiro board PME
- Reutilizar `TargetSiteOnboardingValidator` e `SiteOnboardingChecklist` criados na Story 8.4,
  sem criar lógica paralela de compliance.
- Fechar o onboarding de um `TargetSiteEntity` PME específico, não apenas produzir pesquisa
  documental genérica.
- Selecionar empresas-alvo com foco em Java / backend / plataforma / TI.
- Priorizar boards públicos em Greenhouse; manter Lever e Ashby no backlog imediato.
- Primeiro board recomendado: `bitso` em Greenhouse, por aderência atual ao foco Java/backend no
  board público e disponibilidade de Job Board API oficial em 2026-03-13.
- Registrar `robots.txt`, termos e endpoint público utilizado para o board escolhido.
- **TDD:** teste de metadata / onboarding validation primeiro.

#### Story 9.2 — Implementar `GreenhouseJobBoardClient`
- Consumir `boards-api.greenhouse.io`.
- Buscar vagas publicadas do board escolhido.
- **TDD:** testes com fixture JSON do Job Board API primeiro.

#### Story 9.3 — Implementar `GreenhouseJobNormalizer`
- Mapear payload do Greenhouse para `JobPostingEntity`.
- Cobrir `title`, `company`, `location`, `canonicalUrl`, `publishedAt`, `description`.
- **TDD:** testes de normalização primeiro.

#### Story 9.4 — Implementar `GreenhouseJobScraperStrategy`
- Integrar client + normalizer.
- **TDD:** testes de strategy primeiro.

#### Story 9.5 — Persistir vagas PME via Greenhouse
- Executar fluxo completo: command → strategy → normalize → persist.
- **TDD:** integration test da fatia completa primeiro.
- **Aceite manual obrigatório:** executar um job Greenhouse alinhado ao foco do usuário e validar
  retorno em listagem por data para uma pesquisa como `desenvolvedor de software em java spring boot`.

#### Story 9.6 — Generalizar provider ATS para `LeverPostingsClient`
- Repetir o mesmo padrão em um segundo provider ATS.
- Validar se a abstração do provider permanece estável.
- **TDD:** contract test do provider e testes com fixture JSON primeiro.

---

### Iteration 10 — Processamento assíncrono

#### Story 10.1 — Abstração de fila
- Introduzir contrato de dispatch de job.
- Filas: `static-scrape-jobs`, `api-jobs`, `dynamic-browser-jobs`, `dead-letter-jobs`.
- **TDD:** testes de contrato producer/consumer primeiro.

#### Story 10.2 — Worker de execução
- Consumer resolve strategy e persiste output.
- Enquanto a fila for apenas em memória, o scheduler atua somente como producer e **não** faz
  handoff atômico/desarme do job no banco.
- Duplicações de handoff nessa fase devem ser absorvidas pela Story 10.3 (idempotência), e não por
  uma falsa garantia de durabilidade da fila em memória.
- **TDD:** testes de worker end-to-end primeiro.

#### Story 10.3 — Idempotência e prevenção de duplicatas
- Garantir que eventos repetidos não dupliquem registros.
- Cobrir explicitamente duplicatas vindas do handoff `scheduler -> in-memory queue`.
- Introduzir um `in-flight claim registry` em memória para impedir reenfileiramento do mesmo job
  recorrente dentro do mesmo processo, sem fingir durabilidade.
- Preparar o sistema para futura adoção de handoff durável (`outbox` ou fila persistida) sem mudar
  o contrato funcional dos imports.
- **TDD:** testes de execução duplicada primeiro.

#### Story 10.4 — PersistentCrawlJobQueue no Postgres
- Introduzir storage durável da fila em tabela própria no Postgres.
- Modelar estados mínimos de mensagem: `READY`, `CLAIMED`, `RETRY_WAIT`, `DEAD_LETTER`, `DONE`.
- Persistir payload materializado suficiente para reconstrução do job sem depender de memória local.
- Fechar em fatias:
  - `10.4.1` storage + migration + repository com claim atômico
  - `10.4.2` adaptador `PersistentCrawlJobQueue`
  - `10.4.3` ack/retry/dead-letter persistentes
  - `10.4.4` migração de scheduler/worker para o lifecycle persistente da fila
  - `10.4.5` revisão de simplificação e limpeza
- Estado atual após 10.4.5:
  - `PersistentCrawlJobQueue` é o caminho principal de produção
  - `InMemoryCrawlJobQueue` fica restrita a testes de contrato e cenários locais sem Spring
  - testes de uso cobrem o fluxo `scheduler -> persistent queue -> worker` para sucesso e retry
  - `InFlightCrawlJobRegistry` ainda permanece como mitigação temporária no scheduler e deve ser revisado em limpeza futura, não em stories funcionais imediatas
- **TDD:** entity/repository contract tests primeiro.

#### Gate arquitetural antes das próximas stories funcionais

Antes de seguir o roadmap funcional com segurança, o projeto precisa fechar uma story técnica de
**handoff durável** entre scheduler e worker.

Decisão explícita:

- `InMemoryCrawlJobQueue` + `InFlightCrawlJobRegistry` é apenas mitigação local por processo
- isso reduz duplicatas e looping dentro da instância atual, mas **não** resolve durabilidade após
  restart/crash
- a próxima evolução arquitetural necessária é uma destas opções:
  - fila persistida em tabela no Postgres
  - outbox pattern no Postgres
  - broker externo durável

Recomendação atual do projeto:

- priorizar **Story 10.4 — fila persistida/outbox no Postgres** antes de avançar outras stories funcionais
- só depois reconsiderar remoção de mitigadores em memória
- tratar qualquer continuação sem esse passo como evolução com risco operacional conhecido

---

### Iteration 11 — Fallback dinâmico (Playwright)

**Pré-requisito:** site classificado como Tipo C com caso falhando comprovado.

#### Story 11.1 — Contrato do adaptador Playwright
- Criar `PlaywrightJobFetcher` como implementação de `JobFetcher`.
- **TDD:** contract test com comportamento de página controlado (`WireMock` ou transporte
  equivalente + HTML fake) primeiro.
- Status atual: `PlaywrightJobFetcher` já implementa o contrato com um `PlaywrightBrowserClient` testável e um cliente padrão para Chromium; o teste unitário `PlaywrightJobFetcherTest` cobre o mapeamento do payload.

#### Story 11.2 — Strategy para site dinâmico
- Usar browser fetch somente para sites Tipo C classificados.
- **TDD:** fixture de extração dinâmica falhando primeiro.
- Status atual: `PlaywrightDynamicScraperStrategy` já está implementada, parseia cartões JS-heavy via `DynamicJobListingParser` e falha rapidamente quando o fetch retorna status diferente de 200.

#### Story 11.3 — Bulkhead para browser jobs
- Isolar concorrência de browser dos jobs estáticos.
- **TDD:** testes de limite de concorrência primeiro.

---

### Iteration 12 — Observabilidade e governança

#### Story 12.1 — Métricas e logs estruturados
- Emitir contadores e durações de job.
- **TDD:** testes de emissão de métricas/logs primeiro.

#### Story 12.2 — Checklist de habilitação de site em produção
- Estender o checklist/validator introduzido na Story 8.4, sem duplicar regras paralelas.
- Bloquear ativação sem campos de compliance preenchidos.
- **TDD:** testes de validação primeiro.

#### Story 12.3 — Endpoint de health summary
- `GET /api/v1/scraper/health` — resumo de jobs recentes.
- **TDD:** endpoint tests primeiro.

---

## Definition of Done

Uma story está pronta apenas quando:

1. Testes falhando foram escritos antes do código de produção.
2. Implementação faz os testes passarem.
3. Refatoração concluída.
4. Code review realizado.
5. CI verde.
6. Fixtures e versionamento atualizados quando comportamento de extração mudou.
7. Implicações operacionais documentadas quando relevante.
8. Checklist de onboarding preenchido para qualquer novo site adicionado.
9. Cenário manual de aceite do usuário executado e documentado para a família de fonte alterada.

---

## Order of Delivery Summary

| Ordem | Feature | Razão |
|---|---|---|
| 1 | Foundation + domain model | Base obrigatória para tudo |
| 2 | Indeed MCP integration | API oficial, menor risco legal, maior valor imediato |
| 3 | DOU API integration | Dados públicos, API oficial, concursos federais |
| 4 | Scheduling + listagem por data | Entrega visível para o usuário |
| 5 | Resiliência baseline | Estabilidade antes de novos scrapers |
| 6 | PCI Concursos (HTML estático) | Site mais relevante para concursos, template para demais |
| 7 | Vagas setor privado (HTML estático) | Segundo source de scraping após validação legal |
| 8 | Processamento assíncrono | Escalabilidade após sources validados |
| 9 | Playwright fallback | Somente se Type C comprovado por cenário falhando |
| 10 | Observabilidade e governança | Operacionalização completa |

## References

- ADR001 — Direção tecnológica
- ADR002 — Taxonomia de sites e análise legal
- ADR003 — Stack Java e projetos open source
- ADR004 — Arquitetura de extração
- ADR005 — Modelo de domínio
- ADR006 — Resiliência e rate limiting
- ADR007 — TDD e quality gates
- ADR008 — Observabilidade e governança
- ADR010 — Open source e projetos GitHub
