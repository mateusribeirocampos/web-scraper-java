# STORY-12.7 — Bootstrap de CrawlJob a partir de TargetSite persistido

## Objetivo

Eliminar mais um passo manual do fluxo operacional, permitindo materializar um `CrawlJob`
canônico a partir de um `TargetSite` já persistido, sem SQL manual entre onboarding e execução.

## Ciclo TDD

### Red

- Criados testes para o caso de uso `BootstrapCrawlJobFromTargetSiteUseCase`.
- Criados testes para o endpoint `POST /api/v1/target-sites/{siteId}/bootstrap-crawl-job`.
- O RED inicial falhou por ausência de:
  - caso de uso de bootstrap do job;
  - DTO/resultado de bootstrap;
  - endpoint REST;
  - lookup canônico de `CrawlJob` por `target_site_id`.

### Green

- Implementado bootstrap idempotente por `target_site_id`.
- Exposto endpoint REST para criar/atualizar o `CrawlJob` canônico do site.
- O endpoint retorna:
  - `201 CREATED` quando cria um job novo;
  - `200 OK` quando atualiza um job já existente.

### Refactor

- O bootstrap cria o job com `scheduledAt=now`, `schedulerManaged=true` e `jobCategory=null`,
  deixando a categoria efetiva derivar do `TargetSite`.
- Em updates, o fluxo preserva `scheduledAt`, `schedulerManaged` e `createdAt`, evitando destruir
  a agenda operacional existente.
- Adicionada migration `V009` para garantir unicidade por `target_site_id` em `crawl_jobs`,
  permitindo recovery idempotente em corrida de criação.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/domain/repository/CrawlJobRepository.java`
- `src/main/java/com/campos/webscraper/application/usecase/BootstrappedCrawlJob.java`
- `src/main/java/com/campos/webscraper/application/usecase/BootstrapCrawlJobFromTargetSiteUseCase.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteCrawlJobBootstrapResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/TargetSiteCrawlJobBootstrapController.java`
- `src/main/resources/db/migration/V009__add_unique_target_site_to_crawl_jobs.sql`
- `src/test/java/com/campos/webscraper/application/usecase/BootstrapCrawlJobFromTargetSiteUseCaseTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/TargetSiteCrawlJobBootstrapControllerTest.java`

## Estado final

- `BootstrapCrawlJobFromTargetSiteUseCaseTest` verde
- `TargetSiteCrawlJobBootstrapControllerTest` verde
- `./mvnw -q -DskipTests compile` verde
