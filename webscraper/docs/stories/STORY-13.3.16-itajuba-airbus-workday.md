# Story 13.3.16 — Itajubá privada via Airbus/Helibras Workday

## Objetivo

Implementar a trilha privada de `Itajubá` usando o board oficial `Workday` da `Airbus`, filtrando
as vagas da `Helibras` na cidade para fechar a metade privada do backlog híbrido local.

## Ciclo TDD

1. foram escritos testes para normalização, strategy, import use case e runner do novo provider;
2. a implementação abriu a família `WORKDAY` com client paginado via POST, strategy e import
   idempotente;
3. a suíte focada foi ajustada até validar o catálogo curado, o runner e a serialização do payload;
4. uma regressão extra foi adicionada para o `boardToken` no endpoint de onboarding, porque o
   runtime expôs `null` mesmo com a profile preenchida.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/WorkdayBoardOnboardingProfile.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/WorkdayBoardOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/normalizer/WorkdayJobNormalizer.java`
- `webscraper/src/main/java/com/campos/webscraper/application/strategy/WorkdayJobScraperStrategy.java`
- `webscraper/src/main/java/com/campos/webscraper/application/usecase/WorkdayJobImportUseCase.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/http/WorkdayJobBoardClient.java`
- `webscraper/src/main/java/com/campos/webscraper/interfaces/dto/WorkdayJobPostingResponse.java`
- `webscraper/src/main/java/com/campos/webscraper/interfaces/dto/WorkdayJobsResponse.java`
- `webscraper/src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/GetTargetSiteOnboardingProfileUseCase.java`
- `webscraper/src/test/java/com/campos/webscraper/application/normalizer/WorkdayJobNormalizerTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/strategy/WorkdayJobScraperStrategyTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/usecase/WorkdayJobImportUseCaseTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/GetTargetSiteOnboardingProfileUseCaseTest.java`

## Problemas encontrados

- o endpoint de onboarding expunha `boardToken=null` para `WORKDAY`;
- a validação real precisava de filtro por `locations` no payload do Workday, não apenas paginação;
- o endpoint final `/job-postings` não prova sozinho a fonte consultada, então a validação técnica
  precisou usar o summary do `operational-check`.

## Causa raiz

- o mapper do `GetTargetSiteOnboardingProfileUseCase` só tratava `GREENHOUSE` e `LEVER` como
  famílias com `boardToken`;
- o board `Workday` da Airbus usa POST paginado com facet de localização, diferente dos ATS já
  implementados antes;
- a API de leitura final filtra por perfil funcional do usuário, não por `targetSite`.

## Solução aplicada

- o provider `WORKDAY` foi materializado como nova família operacional;
- o board oficial da Airbus passou a ser consumido por `POST /wday/cxs/.../jobs` com filtro da
  facet de `Itajubá`;
- o endpoint de onboarding agora expõe `boardToken` para `WORKDAY`;
- a profile curada `airbus_helibras_workday` foi aberta como fonte privada oficial da cidade.

## Validação

- testes automatizados:
  - `./mvnw -Dtest=WorkdayJobNormalizerTest,WorkdayJobScraperStrategyTest,WorkdayJobImportUseCaseTest,TargetSiteOnboardingProfileCatalogTest,TargetSiteOnboardingProfileControllerTest,ImportingCrawlJobExecutionRunnerTest test`
- validação real:
  - `GET /actuator/health = UP`
  - `PROFILE_KEY=airbus_helibras_workday KEEP_APP_RUNNING=true ./scripts/run-local-operational-check.sh`
  - `crawlExecutionId=123`
  - `status=SUCCEEDED`
  - `itemsFound=22`

## Lições aprendidas

- `Workday` entra melhor como família própria do que como extensão oportunista de `Lever`;
- o `operational-check` é a evidência correta para validar a fonte específica quando a API final de
  leitura aplica perfis funcionais de busca;
- o catálogo curado precisa de teste direto do mapper, não só de controller mockado.

## Estado final

- trilha privada `airbus_helibras_workday` implementada;
- runtime técnico validado com 22 vagas reais de `Itajubá`;
- pronta para decisão de compliance/ativação.
