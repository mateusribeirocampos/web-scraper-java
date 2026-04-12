# Story 14.1 — Integration stability baseline

## Objetivo

Abrir o novo front pós-cidades pelo ponto mais crítico da sustentação da plataforma: estabilizar a
suíte `integration` e reduzir flakiness residual antes de novas expansões de fonte.

## Ciclo TDD

1. revisar o job `integration` do GitHub Actions e a lista de testes marcados com `@Tag("integration")`;
2. catalogar causas conhecidas de instabilidade;
3. registrar a primeira fatia executável de estabilização antes de abrir correções maiores.

## Arquivos criados / modificados

- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `README.md`
- `webscraper/README.md`
- `webscraper/docs/stories/README.md`

## Problemas encontrados

- o projeto já tem cobertura `integration` valiosa, mas ela depende de Docker/Testcontainers e de
  assumptions de ambiente que historicamente já produziram ruído;
- o backlog anterior estava orientado a fechar cidades, não a consolidar a base de integração.

## Causa raiz

- o crescimento rápido do escopo funcional aumentou a pressão sobre a suíte `integration`;
- os fixes recentes estabilizaram o pipeline, mas ainda falta transformar isso em backlog
  explícito de sustentação.

## Solução aplicada

- o eixo geográfico foi encerrado formalmente no plano;
- o novo front principal do projeto foi registrado com esta ordem:
  1. `integration`/flakiness
  2. onboarding/activation
  3. famílias de fonte
  4. observabilidade/data quality
- a primeira story executável do novo ciclo passa a ser esta baseline de estabilidade de integração.

## Mapeamento atual da suíte `@Tag("integration")`

Cobertura atual identificada:

- aplicação:
  - `WebscraperApplicationTests`
- repositórios:
  - `TargetSiteRepositoryTest`
  - `CrawlJobRepositoryTest`
  - `JobPostingRepositoryTest`
  - `PublicContestPostingRepositoryTest`
  - `PersistentQueueMessageRepositoryTest`
  - `RawSnapshotRepositoryTest`
- import use cases ponta a ponta:
  - `IndeedJobImportUseCaseTest`
  - `DouContestImportUseCaseTest`
  - `GreenhouseJobImportUseCaseTest`
  - `LeverJobImportUseCaseTest`

## Pontos mais frágeis mapeados

1. infraestrutura de container inconsistente
- `TestcontainersConfiguration` usa `postgres:latest`, enquanto vários testes usam `postgres:16`;
- isso abre espaço para drift de versão entre contexto de aplicação e testes de repositório.

2. limpeza manual com ordem de FK espalhada
- vários testes executam `deleteAll()` manual em ordem fixa;
- qualquer novo relacionamento ou cleanup incompleto tende a reabrir violação de FK.

3. uso excessivo de `@SpringBootTest` para testes de persistência
- a suíte de integração inteira sobe contexto Spring completo mesmo quando o foco é repositório;
- isso aumenta custo, tempo de boot e superfície de flakiness.

4. dados temporais não totalmente padronizados
- parte da suíte já migrou para instantes fixos, mas ainda há testes usando `Instant.now()`;
- isso mantém risco residual de asserts frágeis e ordenação instável.

5. dependência estrutural de Docker/Testcontainers
- o job `integration` do CI é valioso, mas o ambiente local continua sujeito a ruído de Docker,
  versão de engine e tempo de bootstrap.

## Primeira correção concreta proposta

Primeira fatia de implementação recomendada:

- unificar a infraestrutura de Testcontainers do projeto em um único baseline determinístico:
  - remover `postgres:latest` de `TestcontainersConfiguration`;
  - padronizar a imagem em `postgres:16` em toda a suíte `integration`;
  - centralizar essa definição para impedir drift futuro entre testes.

Razão da escolha:

- é a correção de menor risco e maior impacto sistêmico;
- reduz instabilidade de ambiente antes de mexer em cleanup transacional ou reestruturação de
  contexto Spring;
- prepara a base para uma segunda fatia focada em cleanup/fixtures compartilhados.

## Segunda correção concreta aplicada

- foi criada uma base compartilhada para testes de repositório em
  `AbstractRepositoryIntegrationTest`;
- o cleanup manual por `deleteAll()` em ordem fixa foi substituído por helpers baseados em
  `TRUNCATE ... RESTART IDENTITY CASCADE` via `JdbcTemplate`;
- os testes de `domain/repository` passaram a reutilizar esse fixture, reduzindo repetição e
  sensibilidade a mudanças de FK entre `target_sites`, `crawl_jobs`, `crawl_executions`,
  `job_postings` e `public_contest_postings`.

Arquivos da segunda fatia:

- `webscraper/src/test/java/com/campos/webscraper/domain/repository/AbstractRepositoryIntegrationTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/TargetSiteRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/CrawlJobRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/JobPostingRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/PublicContestPostingRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/PersistentQueueMessageRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/RawSnapshotRepositoryTest.java`

## Terceira correção concreta aplicada

- o projeto não expõe `@DataJpaTest` no classpath atual de Spring Boot 4 usado aqui, então a
  redução de contexto foi feita por um caminho compatível com o stack real;
- foi criado um slice mínimo de persistência para testes de repositório:
  - `RepositoryPersistenceTestApplication`
  - `RepositoryPersistenceTest`
- os testes de `domain/repository` deixaram de subir a aplicação principal inteira e passaram a
  bootar apenas auto-configuração + entidades + repositories JPA.

Arquivos da terceira fatia:

- `webscraper/src/test/java/com/campos/webscraper/domain/repository/RepositoryPersistenceTestApplication.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/RepositoryPersistenceTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/TargetSiteRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/CrawlJobRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/JobPostingRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/PublicContestPostingRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/PersistentQueueMessageRepositoryTest.java`
- `webscraper/src/test/java/com/campos/webscraper/domain/repository/RawSnapshotRepositoryTest.java`

## Validação

- evidências revisadas:
  - `.github/workflows/ci.yaml`
  - testes `@Tag("integration")` em `domain/repository` e `application/usecase`
  - histórico recente de correções em repository tests e job split do Actions
  - `TestcontainersConfiguration`
  - `CrawlJobRepositoryTest`
  - `TargetSiteRepositoryTest`

## Lições aprendidas

- depois de fechar um eixo grande de produto, o backlog precisa trocar de direção explicitamente;
- sem um front de sustentação, a velocidade de novas fontes cai porque a base operacional deixa de
  ser previsível.

## Estado final

- novo front pós-cidades aberto;
- suíte `integration` mapeada;
- pontos frágeis principais explicitados;
- primeira correção concreta definida para implementação.
