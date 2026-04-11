# Story 13.3.25 — Extrema privada via Special Dog Company Gupy

## Objetivo

Implementar a trilha privada de `Extrema` usando a API pública da `Gupy`, isolando o board oficial
da `Special Dog Company`.

## Ciclo TDD

1. abrir profile curada e cobertura do cliente/runner/catálogo;
2. introduzir o filtro local por identidade do board;
3. validar a trilha com `operational-check` real.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/GupyBoardOnboardingProfile.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/GupyBoardOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/http/GupyJobBoardClient.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/GetTargetSiteOnboardingProfileUseCase.java`
- `webscraper/src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java`
- `webscraper/src/main/resources/db/migration/V019__approve_gupy_specialdog_extrema.sql`
- `webscraper/src/test/java/com/campos/webscraper/infrastructure/http/GupyJobBoardClientTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/GetTargetSiteOnboardingProfileUseCaseTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunnerTest.java`

## Problemas encontrados

- a API pública da `Gupy` não isola sozinha o board correto apenas com `jobName=Extrema`;
- sem filtro adicional, a cidade podia misturar vagas de outros boards.

## Causa raiz

- a busca pública da `Gupy` é ampla e orientada a relevância;
- o board oficial da `Special Dog Company` precisava de uma segunda camada de identificação.

## Solução aplicada

- `gupy_specialdog_extrema` entrou como profile curada oficial da cidade;
- o `GupyJobBoardClient` ganhou filtro local por `careerPageName`, preservando só os itens do
  board da `Special Dog Company`;
- o endpoint de onboarding passou a expor corretamente `boardToken` para a nova família `GUPY`.

## Validação

- testes automatizados:
  - `./mvnw -Dtest=GupyJobBoardClientTest,GetTargetSiteOnboardingProfileUseCaseTest,TargetSiteOnboardingProfileCatalogTest,ImportingCrawlJobExecutionRunnerTest test`
- validação real:
  - `GET /actuator/health = UP`
  - `POST /api/v1/onboarding-profiles/gupy_specialdog_extrema/operational-check?smokeRun=true&daysBack=60`
  - resultado: `crawlExecutionId=129`, `status=SUCCEEDED`, `itemsFound=1`

## Lições aprendidas

- para `Gupy`, a facet de cidade nem sempre basta; a identidade do board precisa ser explicitada;
- um filtro local leve resolve a ambiguidade sem abrir uma nova família ATS.

## Estado final

- `gupy_specialdog_extrema` implementada;
- trilha privada validada em runtime real e pronta para ativação/compliance.
