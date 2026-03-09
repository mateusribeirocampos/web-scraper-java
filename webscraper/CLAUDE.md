# CLAUDE.md — WebScraper de Vagas Java Júnior / Spring Boot

Este arquivo é lido automaticamente por Claude em cada sessão neste projeto.
Contém o contexto arquitetural, regras de desenvolvimento e comandos essenciais.

---

## O que é este projeto

Plataforma backend em Java / Spring Boot para coletar, normalizar e disponibilizar
vagas de emprego (setor privado) e concursos públicos brasileiros, filtradas para
a stack **Java Júnior / Spring Boot**.

Duas categorias de fontes de dados:
- `PRIVATE_SECTOR` — vagas em portais como Indeed, Vagas.com, Gupy portal.
- `PUBLIC_CONTEST` — concursos públicos do DOU (Diário Oficial da União) e PCI Concursos.

**Princípio central:** API oficial sempre tem prioridade absoluta sobre scraping HTML.

---

## Stack de produção

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 21 (Virtual Threads habilitadas) |
| Framework | Spring Boot 4.0.3 |
| Build | Maven |
| Parsing HTML | jsoup 1.18.3 |
| HTTP Client | OkHttp 4.12.0 |
| Browser Automation (fallback) | Playwright for Java 1.49.0 |
| Persistência | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Resiliência | Resilience4j (via Spring Cloud) |
| Testes | JUnit 5 + Mockito + Testcontainers + WireMock + AssertJ |

---

## Regra de TDD — INVIOLÁVEL

**Nenhuma feature começa pela implementação de produção.**

O ciclo obrigatório em toda story:
1. Escrever o teste **falhando** primeiro (`@Test` + `assertThrows` ou `assertEquals`).
2. Implementar o mínimo para o teste **passar** (sem over-engineering).
3. **Refatorar** com todos os testes verdes.
4. Code review antes do merge.

Se Claude for convidado a implementar uma feature, deve perguntar:
> "Posso começar pelo teste falhando antes de escrever o código de produção?"

---

## Arquitetura em camadas

```
src/main/java/com/campos/webscraper/
├── domain/              # Entidades de negócio, value objects, enums, interfaces de repositório
│   ├── model/           # JobPostingEntity, PublicContestPostingEntity, TargetSiteEntity, etc.
│   ├── enums/           # JobContractType, SeniorityLevel, GovernmentLevel, ContestStatus, etc.
│   └── repository/      # Interfaces de repositório (Spring Data JPA)
├── application/         # Use cases, orchestrators, normalizers, factories
│   ├── usecase/         # CrawlJobOrchestrator, JobPersistenceService, etc.
│   ├── strategy/        # JobScraperStrategy (interface) + implementações por site
│   ├── factory/         # JobScraperFactory
│   └── normalizer/      # IndeedJobNormalizer, PciConcursosNormalizer, etc.
├── infrastructure/      # Adaptadores externos: HTTP, banco, filas, browser
│   ├── http/            # HttpJobFetcher (OkHttp), IndeedApiClient, DouApiClient
│   ├── browser/         # PlaywrightJobFetcher (somente Tipo C)
│   ├── persistence/     # Implementações JPA dos repositórios
│   └── scheduler/       # CrawlJobScheduler (@Scheduled)
├── interfaces/          # Controllers REST, DTOs de entrada/saída
│   ├── rest/            # JobPostingController, PublicContestController
│   └── dto/             # JobPostingResponse, ContestPostingResponse, etc.
└── shared/              # Utilities, exceções customizadas, fingerprint calculator
```

---

## Entidades principais do domínio

### JobPosting (setor privado)
Campos obrigatórios: `title`, `company`, `canonicalUrl`, `publishedAt`, `fingerprintHash`.
Campos importantes: `seniority` (JUNIOR/MID/SENIOR), `contractType` (CLT/PJ), `remote`, `techStackTags`.

### PublicContestPosting (concursos públicos)
Campos obrigatórios: `contestName`, `organizer`, `publishedAt`, `fingerprintHash`.
Campos importantes: `registrationEndDate`, `numberOfVacancies`, `contestStatus`, `governmentLevel`.

**`publishedAt: LocalDate` é OBRIGATÓRIO em ambas as entidades** — toda vaga deve ser
disponibilizável e ordenável por data de publicação.

---

## Padrões de design fundamentais

### Strategy + Factory (extração por site)
Cada site tem sua `JobScraperStrategy`. A `JobScraperFactory` resolve qual strategy usar
com base nos metadados do `TargetSite`. Novos sites = nova strategy, sem tocar no pipeline.

### API-first obrigatório
Antes de implementar qualquer scraper HTML, verificar se existe:
- API oficial (Indeed MCP Connector, DOU API gov)
- API pública documentada (ex.: Gupy API, Codante Jobs API)

Scraping HTML é sempre o último recurso.

### Deduplicação por fingerprint
Toda entidade de vaga calcula `fingerprintHash` com SHA-256 de campos estáveis (siteCode,
externalId, title, company, publishedAt). Isso evita duplicatas entre execuções.

---

## Sites e estratégias (resumo ADR002)

| Site | Tipo | Estratégia |
|---|---|---|
| Indeed Brasil | API (Tipo E) | `IndeedApiJobScraperStrategy` — MCP Connector oficial |
| DOU (in.gov.br) | API (Tipo E) | `DouApiContestScraperStrategy` — API gov pública |
| PCI Concursos | HTML estático (Tipo A) | `PciConcursosScraperStrategy` — jsoup (após revisão legal) |
| LinkedIn | Tipo D | ❌ Excluído — ToS proíbe scraping |
| Catho / Glassdoor | Tipo B/C | ❌ Excluídos fase 1 — ToS proíbe |

**NUNCA** ativar um site em produção sem o checklist legal preenchido (ADR002, Seção 3).

---

## Comandos essenciais

```bash
# Compilar o projeto
./mvnw compile

# Executar todos os testes (requer Docker para Testcontainers)
./mvnw test

# Executar apenas testes unitários (sem Docker)
./mvnw test -Dgroups="unit"

# Executar testes de integração com Testcontainers
./mvnw test -Dgroups="integration"

# Build completo (compile + test + package)
./mvnw clean package

# Rodar a aplicação localmente (requer PostgreSQL rodando)
./mvnw spring-boot:run

# Verificar dependências desatualizadas
./mvnw versions:display-dependency-updates

# Flyway: verificar status das migrations
./mvnw flyway:info

# Flyway: executar migrations manualmente
./mvnw flyway:migrate
```

---

## Migrations Flyway

Localização: `src/main/resources/db/migration/`

Convenção de nomenclatura:
```
V001__create_target_sites.sql
V002__create_crawl_jobs_and_executions.sql
V003__create_raw_snapshots.sql
V004__create_job_postings.sql
V005__create_public_contest_postings.sql
V006__create_selector_bundles.sql
```

**Regra:** nunca editar uma migration já aplicada em produção. Sempre criar nova versão.

---

## Configuração local de desenvolvimento

Para rodar localmente, é necessário um PostgreSQL. Use Docker Compose:

```yaml
# docker-compose.dev.yml (criar na raiz do projeto quando necessário)
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: webscraper
      POSTGRES_USER: webscraper
      POSTGRES_PASSWORD: webscraper
    ports:
      - "5432:5432"
```

Testcontainers nos testes de integração sobe o PostgreSQL automaticamente via Docker.
Para testes unitários rápidos (sem Docker), use H2 em memória.

---

## Regras de código limpo (Clean Code)

- Interfaces com nomes descritivos: `JobScraperStrategy`, `JobFetcher`, `JobNormalizer`.
- Implementações com nomes específicos: `IndeedApiJobScraperStrategy`, `HttpJobFetcher`.
- Use Lombok para reduzir boilerplate (`@Builder`, `@RequiredArgsConstructor`, `@Getter`).
- Value objects imutáveis: `ScrapeCommand`, `ScrapeResult<T>`, `FetchRequest`, `FetchedPage`.
- Exceções com mensagens descritivas: `UnsupportedSiteException("Site [X] has no registered strategy")`.
- Nenhum `System.out.println` — sempre `SLF4J` com `@Slf4j`.
- Constantes de configuração em `application.properties`, nunca hard-coded.

---

## O que Claude NÃO deve fazer neste projeto

- **Não** criar starters Spring Boot que não existem (ex.: `spring-boot-starter-flyway`).
- **Não** usar `spring-ai-jsoup-document-reader` para scraping — é para Spring AI/LLM.
- **Não** ativar um site em produção sem checklist de permissão preenchido (ADR002).
- **Não** implementar features sem escrever o teste falhando primeiro.
- **Não** usar Python para nenhum componente de produção (ADR003).
- **Não** adicionar Playwright para sites que ainda não foram comprovadamente Tipo C.
- **Não** fazer scraping de sites classificados como SCRAPING_PROIBIDO (LinkedIn, Catho, Glassdoor).
- **Não** hard-codar seletores CSS — sempre usar `SelectorBundle` versionado.
- **Não** editar migrations Flyway já aplicadas.

---

## ADRs de referência

Os documentos de decisão arquitetural estão em:
`/Users/mateusribeirodecampos/development/Projects/vagas-scrap/ADRs-web-scraping-vagas/`

| ADR | Assunto |
|---|---|
| ADR001 | Direção arquitetural e stack de tecnologia |
| ADR002 | Sites brasileiros: análise legal, robots.txt, APIs disponíveis |
| ADR003 | Java vs Python — decisão definitiva com tabela de pros/contras |
| ADR004 | Arquitetura de extração: Strategy, Factory, Fetcher, Normalizer |
| ADR005 | Modelo de domínio JPA: JobPosting, PublicContestPosting |
| ADR006 | Resiliência: Retry, RateLimiter, Bulkhead, CircuitBreaker |
| ADR007 | TDD e quality gates |
| ADR008 | Observabilidade, segurança e governança |
| ADR009 | Plano XP: 12 iterations com stories e TDD explícito |
| ADR010 | Open source no GitHub: projetos de referência (JobSpy, PCIConcursos) |

---

## Ordem de implementação (ADR009 — Iteration 1 primeiro)

1. Foundation: domain model + enums + value objects (com testes de invariantes).
2. IndeedApiJobScraperStrategy — primeira integração API-first.
3. DouApiContestScraperStrategy — concursos federais via API gov.
4. Scheduling + endpoint de listagem por data.
5. Resiliência baseline (Retry, RateLimiter).
6. PCI Concursos — primeiro scraper HTML (após revisão legal formal).
