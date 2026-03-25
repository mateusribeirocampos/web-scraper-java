# WebScraper Java

Plataforma backend em Java 21 + Spring Boot para coleta, normalizacao, persistencia e consulta de
vagas privadas e concursos publicos, com prioridade absoluta para fontes API-first e fallback
controlado para scraping HTML ou browser somente quando permitido.

O projeto foi construido com TDD e evoluido por iteracoes documentadas em ADRs e story logs. Hoje
ele ja cobre fontes oficiais/publicas, fontes ATS, scraping HTML estatico, fallback dinamico com
Playwright e execucao assíncrona com fila persistida.

## Objetivos

- Coletar vagas de tecnologia e concursos publicos com foco pratico em Java / Spring Boot.
- Priorizar fontes de menor risco legal: APIs oficiais e boards publicos.
- Persistir dados de forma idempotente para evitar duplicatas entre execucoes.
- Permitir execucao manual, agendada e assíncrona dos `CrawlJob`s.
- Validar utilidade real do sistema com um fluxo manual reproduzivel de ponta a ponta.

## Stack

- Java 21
- Maven Wrapper (`./mvnw`)
- Spring Boot 4.0.3
- Spring Data JPA + Hibernate
- PostgreSQL + Flyway
- Resilience4j
- OkHttp + jsoup
- Playwright for Java
- JUnit 5, Spring Boot Test, WireMock e Testcontainers

## Principios Arquiteturais

- API-first obrigatorio: API oficial sempre tem precedencia sobre scraping.
- TDD mandatorio: toda feature nasce de teste falhando.
- Gate legal: onboarding exige classificacao tecnica e validacao legal.
- Persistencia separada da extracao: strategies, normalizers e use cases tem papeis distintos.
- Operacao rastreavel: `CrawlJob`, `CrawlExecution`, fila persistida e dead-letter fazem parte do modelo.

## Fontes Atualmente Cobertas

| Familia | Tipo principal | Status |
|---|---|---|
| Indeed Brasil | API oficial | Implementada |
| Diario Oficial da Uniao | API publica | Implementada |
| PCI Concursos | HTML estatico | Implementada |
| Greenhouse | ATS / board publico | Implementada |
| Gupy | ATS / board publico | Implementada |
| Playwright fallback | Site dinamico Type C | Implementado |

## Visao de Arquitetura

```mermaid
flowchart LR
    A[Scheduler ou endpoint manual de execucao] --> B[CrawlJobDispatcher]
    B --> C[PersistentCrawlJobQueue]
    C --> D[CrawlJobWorker]
    D --> E[ImportingCrawlJobExecutionRunner]
    E --> F[JobScraperFactory]
    F --> G[Strategies]
    G --> H[Clients / Fetchers / Parsers]
    H --> I[Normalizers]
    I --> J[Import Use Cases]
    J --> K[(PostgreSQL)]
    K --> L[Query Controllers]
```

## Fluxo de Execucao

```mermaid
sequenceDiagram
    participant U as Usuario/Scheduler
    participant API as REST Controller
    participant Q as Persistent Queue
    participant W as Worker
    participant R as Runner
    participant S as Strategy
    participant DB as PostgreSQL

    U->>API: POST execute crawl job
    API->>Q: enqueue(job)
    W->>Q: claim next message
    W->>R: run(crawlJob, crawlExecution)
    R->>S: scrape(command)
    S->>DB: persist normalized items
    W->>Q: ack / retry / dead-letter
```

## Estrutura do Repositorio

```text
web-scraper-java/
├── ADRs-web-scraping-vagas/    # decisoes arquiteturais e plano XP
└── webscraper/                 # aplicacao Spring Boot
    ├── src/main/java/com/campos/webscraper
    │   ├── application/        # strategies, use cases, factory, queue, orchestrators
    │   ├── config/             # beans e configuracoes Spring
    │   ├── domain/             # enums, entidades e repositorios
    │   ├── infrastructure/     # http, browser, parser, persistence
    │   ├── interfaces/         # REST, DTOs e scheduler adapters
    │   └── shared/             # utilitarios e excecoes
    ├── src/main/resources/     # application-*.properties, migrations e seeds
    └── docs/stories/           # logs detalhados por story
```

## Funcionalidades Implementadas

- Modelo de dominio JPA para `TargetSite`, `CrawlJob`, `CrawlExecution`, `JobPosting` e `PublicContestPosting`.
- Onboarding legal/técnico de fontes com validator dedicado.
- Strategies por familia de fonte, resolvidas por factory.
- Persistencia idempotente para evitar duplicatas em `job_postings` e `public_contest_postings`.
- Reenriquecimento idempotente de `job_postings` existentes quando reruns trazem payload mais rico.
- Scheduler, trigger manual via REST e worker assíncrono.
- Fila persistida em Postgres com claim, ack, retry e dead-letter.
- Retry, rate limiting, bulkhead e circuit breaker por fonte.
- Gate operacional para ativacao de `TargetSite` via checklist consumido pela aplicacao.
- Assistencia de compliance para prefill de evidencias de `robots.txt`, ToS e endpoint oficial antes da ativacao.
- Bootstrap de `TargetSite` a partir de perfis curados para reduzir setup manual antes da ativacao.
- Bootstrap de `CrawlJob` canônico a partir de `TargetSite` persistido.
- Orquestracao unica por `profileKey` para bootstrap de `TargetSite` + `CrawlJob` + smoke run opcional.
- Consulta REST para vagas privadas e concursos.
- Story logs e ADRs sincronizados com o estado do projeto.

## Endpoints Disponiveis

| Metodo | Endpoint | Uso |
|---|---|---|
| `POST` | `/api/v1/crawl-jobs/{jobId}/execute` | Dispara execucao manual de um `CrawlJob` |
| `POST` | `/api/v1/target-sites/{siteId}/activation` | Aplica o checklist de onboarding e so habilita o site se a compliance fechar |
| `GET` | `/api/v1/target-sites/{siteId}/activation-assistance` | Gera um draft assistido de compliance com evidencias curadas ou derivadas e mostra os gaps que ainda bloqueiam a ativacao |
| `POST` | `/api/v1/target-sites/{siteId}/bootstrap-crawl-job` | Cria ou atualiza o `CrawlJob` canônico do site persistido |
| `POST` | `/api/v1/target-sites/{siteId}/smoke-run` | Executa um smoke run controlado one-off a partir do job canônico, com guarda contra duplicidade e retorno de `smokeRunStatus` |
| `GET` | `/api/v1/onboarding-profiles` | Lista perfis operacionais de onboarding curados por fonte |
| `GET` | `/api/v1/onboarding-profiles/{profileKey}` | Retorna o checklist operacional completo de um perfil curado |
| `POST` | `/api/v1/onboarding-profiles/{profileKey}/bootstrap?smokeRun=false` | Orquestra em uma chamada o bootstrap do `TargetSite` e do `CrawlJob` canônico |
| `POST` | `/api/v1/onboarding-profiles/{profileKey}/bootstrap?smokeRun=true` | Orquestra bootstrap do `TargetSite`, bootstrap do `CrawlJob` e smoke run one-off opcional |
| `POST` | `/api/v1/onboarding-profiles/{profileKey}/bootstrap-target-site` | Cria ou atualiza um `TargetSite` persistido a partir do perfil curado |
| `GET` | `/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND` | Lista vagas privadas recentes usando um perfil explícito; `since` continua aceito e sobrescreve `daysBack` |
| `GET` | `/api/v1/public-contests?status=...&orderBy=...` | Lista concursos publicos |
| `GET` | `/api/v1/scraper/health` | Retorna resumo operacional de execucoes recentes e situacao agregada da fila persistida |

## Como Rodar Localmente

### 1. Pre-requisitos

- JDK 21
- Maven Wrapper do projeto
- PostgreSQL local
- Docker funcional apenas se voce for rodar testes de integracao com Testcontainers

### 2. Banco local

O perfil `dev` aponta para um PostgreSQL local em `jdbc:postgresql://localhost:5432/webscraper`.

Revise:

- `webscraper/src/main/resources/application.properties`
- `webscraper/src/main/resources/application-dev.properties`

Se necessario, ajuste usuario/senha do datasource local antes de subir a aplicacao.

### 3. Subir a aplicacao

```bash
cd webscraper
./mvnw spring-boot:run
```

Por padrao o perfil ativo e `dev`.

### 4. Rodar testes

Testes unitarios/rapidos:

```bash
cd webscraper
./mvnw test -DexcludedGroups=integration
```

Teste especifico:

```bash
cd webscraper
./mvnw -Dtest=IndeedJobImportUseCaseTest test
```

Observacao: testes com Testcontainers exigem Docker acessivel e com API compativel com a versao
do Testcontainers usada pelo projeto.

## Validacao Manual Oficial

O procedimento oficial atual para validar o fluxo de ponta a ponta da familia Gupy e:

1. disparar os `CrawlJob`s persistidos via endpoint manual;
2. aguardar o `DISPATCHED` e a conclusao da execucao;
3. consultar apenas vagas recentes por uma intencao de busca real do usuario.

### Disparo manual dos jobs

```bash
for id in 15 16 17 18; do
  echo -n "Job $id:"
  curl -s -X POST http://localhost:8080/api/v1/crawl-jobs/$id/execute
  echo ""
done
```

Resposta esperada:

```json
{"jobId":15,"status":"DISPATCHED"}
```

Esse retorno nao significa que a raspagem terminou. Ele apenas confirma que o comando foi aceito e
encaminhado ao pipeline de execucao.

### Consulta funcional no banco

```sql
SELECT title, company, seniority, tech_stack_tags, canonical_url
FROM job_postings
WHERE published_at >= CURRENT_DATE - INTERVAL '60 days'
  AND (application_deadline IS NULL OR application_deadline >= CURRENT_DATE)
  AND lower(coalesce(title, '') || ' ' || coalesce(company, '') || ' ' || coalesce(canonical_url, ''))
      !~ '(banco de talentos|banco talentos|talent pool|talent community|talent network)'
  AND coalesce(seniority::text, '') NOT IN ('SENIOR', 'LEAD')
  AND lower(coalesce(title, '') || ' ' || coalesce(tech_stack_tags, '') || ' ' || coalesce(description, ''))
      ~ '(^|[^a-z])(java|spring|kotlin)([^a-z]|$)'
  AND lower(coalesce(title, '') || ' ' || coalesce(description, ''))
      ~ '(backend|back-end|desenvolvedor|developer|software engineer|engenheiro de software|programador)'
  AND lower(coalesce(title, '') || ' ' || coalesce(description, ''))
      !~ '(manager|gerente|lead|lider|principal|staff|architect|arquiteto|head|director|diretor|coordinator|coordenador)'
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

Esse teste valida o caminho completo:

- `CrawlJob` persistido
- dispatch manual
- execucao do worker
- strategy e import use case da fonte
- persistencia em `job_postings`
- consulta final orientada a uma busca real

### Consulta oficial por endpoint

Para consumo da API da aplicacao, o caminho oficial agora ja trata recencia como criterio obrigatorio
de utilidade. O perfil default tambem exclui banco de talentos, corta senioridade `SENIOR/LEAD`,
bloqueia cargos de gestao/lideranca e exige sinal real de stack (`Java`, `Spring` ou `Kotlin`)
mais sinal de funcao aderente (`backend`, `developer`, `software engineer`, `desenvolvedor`,
etc.) para evitar falso positivo por titulo generico:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND"
```

Exemplo com filtro adicional de senioridade:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND&seniority=JUNIOR"
```

Perfil intermediario, menos rigido que o default e ainda protegido contra banco de talentos
e cargos de lideranca:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_BACKEND_BALANCED"
```

Perfil pragmatico, para volume real com stack aderente e sem banco de talentos, mesmo que a vaga
nao seja um fit estrito de funcao:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_STACK_PRAGMATIC"
```

Se voce quiser uma leitura exploratoria mais ampla, sem o perfil estrito de aderencia:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=UNFILTERED"
```

## Qualidade Atual dos Dados

- Boards Greenhouse materializados no projeto usam `?content=true` para trazer `description`
  completa no payload.
- Reruns Greenhouse agora atualizam `description`, `tech_stack_tags`, `payload_json` e outros
  campos enriqueciveis em registros ja existentes com o mesmo fingerprint.
- As heuristicas de `Go` e `Python` foram endurecidas para exigir contexto tecnico suficiente,
  reduzindo falso positivo em vagas de sales, ops, controllership e business.
- Quando a base recente esta escassa, o perfil recomendado para ganhar volume sem cair no
  `UNFILTERED` continua sendo `JAVA_STACK_PRAGMATIC`.

## Diagramas de Componentes

```mermaid
flowchart TD
    subgraph Inbound
        A1[Scheduler]
        A2[CrawlJobController]
    end

    subgraph Orchestration
        B1[CrawlJobDispatcher]
        B2[PersistentCrawlJobQueue]
        B3[CrawlJobWorker]
        B4[ImportingCrawlJobExecutionRunner]
    end

    subgraph Extraction
        C1[DefaultJobScraperFactory]
        C2[Indeed / DOU / PCI]
        C3[Greenhouse / Gupy]
        C4[PlaywrightDynamicScraperStrategy]
    end

    subgraph Persistence
        D1[CrawlExecutionRepository]
        D2[JobPostingRepository]
        D3[PublicContestPostingRepository]
        D4[PersistentQueueMessageRepository]
    end

    A1 --> B1
    A2 --> B1
    B1 --> B2
    B2 --> B3
    B3 --> B4
    B4 --> C1
    C1 --> C2
    C1 --> C3
    C1 --> C4
    B4 --> D1
    B4 --> D2
    B4 --> D3
    B2 --> D4
```

## Documentacao do Projeto

- [Resumo executivo dos ADRs](ADRs-web-scraping-vagas/ADR-Summary-of-WebScraper-ADRs.md)
- [ADR002 - Taxonomia e requisitos](ADRs-web-scraping-vagas/ADR002-Target-Site-Taxonomy-and-Requirements-for-WebScraper.md)
- [ADR009 - Plano XP e tarefas](ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md)
- [Story logs](webscraper/docs/stories/README.md)
- [README operacional do modulo Spring Boot](webscraper/README.md)

## Estado Atual e Proximo Passo

O projeto ja consolidou as iteracoes de foundation, fontes API/publicas, ATSs principais,
Playwright fallback e fila persistida. O foco natural daqui para frente e continuar a trilha de
observabilidade/governanca e seguir expandindo as familias de fonte com o mesmo fluxo:

1. TDD
2. implementacao
3. review
4. documentacao
5. validacao manual oficial
