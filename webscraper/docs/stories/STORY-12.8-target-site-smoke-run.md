# STORY-12.8 — Smoke run operacional por TargetSite

## Objetivo

Executar um primeiro dispatch controlado de uma fonte recém-bootstrapada, reduzindo o passo manual
entre onboarding operacional e validação inicial de funcionamento.

## Ciclo TDD

### Red

- Criados testes para o caso de uso `RunTargetSiteSmokeRunUseCase`.
- Criados testes para o endpoint `POST /api/v1/target-sites/{siteId}/smoke-run`.
- O RED inicial falhou por ausência de:
  - caso de uso do smoke run;
  - DTO/resultado de resposta;
  - controller REST.

### Green

- Implementado `RunTargetSiteSmokeRunUseCase`.
- Exposto endpoint de smoke run sobre `TargetSite`.
- O fluxo reaproveita o bootstrap canônico de `CrawlJob` e faz dispatch imediato via
  `CrawlJobDispatcher`.
- O smoke run coordena com `InFlightCrawlJobRegistry` para não executar em paralelo um job canônico
  já em voo.
- Quando executa, o smoke run materializa um `CrawlJob` transitório (`schedulerManaged=false`) para
  a verificação one-off, sem mutar `scheduledAt` do job canônico.

### Refactor

- O smoke run não reimplementa bootstrap nem dispatch.
- O smoke run usa o job canônico como referência operacional, mas executa um job transitório para
  não alterar a agenda regular.
- O retorno expõe:
  - `siteId`
  - `siteCode`
  - `jobId`
  - `bootstrapStatus`
  - `smokeRunStatus`
  - `dispatchStatus`
- `smokeRunStatus=DISPATCHED` quando o dispatch foi realmente executado.
- `smokeRunStatus=SKIPPED_IN_FLIGHT` quando o job canônico já estava claimado/em voo.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/usecase/RunTargetSiteSmokeRunUseCase.java`
- `src/main/java/com/campos/webscraper/application/usecase/TargetSiteSmokeRunResult.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteSmokeRunResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/TargetSiteSmokeRunController.java`
- `src/test/java/com/campos/webscraper/application/usecase/RunTargetSiteSmokeRunUseCaseTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/TargetSiteSmokeRunControllerTest.java`

## Estado final

- `RunTargetSiteSmokeRunUseCaseTest` verde
- `TargetSiteSmokeRunControllerTest` verde
- `./mvnw -q -DskipTests compile` verde
