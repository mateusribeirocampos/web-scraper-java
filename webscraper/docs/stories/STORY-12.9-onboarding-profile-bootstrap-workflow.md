# STORY-12.9 — Orquestracao unificada de onboarding por profileKey

## Objetivo

Reduzir o numero de chamadas operacionais no onboarding curado, permitindo materializar o
`TargetSite`, o `CrawlJob` canônico e um smoke run opcional a partir de um unico `profileKey`.

## Ciclo TDD

### Red

- Criados testes para o caso de uso `BootstrapOnboardingProfileWorkflowUseCase`.
- Criados testes para o endpoint `POST /api/v1/onboarding-profiles/{profileKey}/bootstrap`.
- O RED inicial falhou por ausencia de:
  - caso de uso orquestrador;
  - DTO consolidado de resposta;
  - controller REST unificado.

### Green

- Implementado `BootstrapOnboardingProfileWorkflowUseCase`.
- Exposto endpoint `POST /api/v1/onboarding-profiles/{profileKey}/bootstrap`.
- O fluxo:
  - bootstrapa o `TargetSite` pelo perfil curado;
  - bootstrapa o `CrawlJob` canônico pelo `siteId` persistido;
  - opcionalmente dispara o smoke run quando `smokeRun=true`.

### Refactor

- A 12.9 nao duplica regra dos passos individuais.
- A orquestracao delega para os casos de uso ja existentes:
  - `BootstrapTargetSiteFromProfileUseCase`
  - `BootstrapCrawlJobFromTargetSiteUseCase`
  - `RunTargetSiteSmokeRunUseCase`
- A resposta consolidada expoe:
  - `profileKey`
  - `targetSiteBootstrapStatus`
  - `siteId`
  - `siteCode`
  - `enabled`
  - `legalStatus`
  - `crawlJobBootstrapStatus`
  - `crawlJobId`
  - `schedulerManaged`
  - `scheduledAt`
  - `smokeRunRequested`
  - `smokeRunStatus`
  - `smokeRunDispatchStatus`
  - `smokeRunJobId`

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/onboarding/BootstrapOnboardingProfileWorkflowUseCase.java`
- `src/main/java/com/campos/webscraper/application/onboarding/BootstrappedOnboardingWorkflowResult.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/OnboardingProfileBootstrapWorkflowResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/OnboardingProfileBootstrapWorkflowController.java`
- `src/test/java/com/campos/webscraper/application/onboarding/BootstrapOnboardingProfileWorkflowUseCaseTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/OnboardingProfileBootstrapWorkflowControllerTest.java`

## Estado final

- `BootstrapOnboardingProfileWorkflowUseCaseTest` verde
- `OnboardingProfileBootstrapWorkflowControllerTest` verde
- `./mvnw -q -DskipTests compile` verde
