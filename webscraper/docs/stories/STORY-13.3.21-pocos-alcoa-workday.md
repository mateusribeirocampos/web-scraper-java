# Story 13.3.21 — Poços de Caldas privada via Alcoa Workday

## Objetivo

Implementar a trilha privada de `Poços de Caldas` usando o board oficial `Workday` da `Alcoa`.

## Ciclo TDD

1. abrir testes para normalizer, strategy, import use case, catálogo e runner da nova profile;
2. estender a família `WORKDAY` com a facet de `Poços de Caldas`;
3. validar a execução com a aplicação rodando.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/WorkdayBoardOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/normalizer/WorkdayJobNormalizer.java`
- `webscraper/src/main/java/com/campos/webscraper/application/strategy/WorkdayJobScraperStrategy.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `webscraper/src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java`
- `webscraper/src/test/java/com/campos/webscraper/application/normalizer/WorkdayJobNormalizerTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/strategy/WorkdayJobScraperStrategyTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/usecase/WorkdayJobImportUseCaseTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunnerTest.java`

## Problemas encontrados

- a família `WORKDAY` só conhecia a facet de `Itajubá`;
- a nova cidade exigia um segundo mapeamento de board root, company alias e location facet.

## Causa raiz

- o primeiro slice de `WORKDAY` foi aberto estritamente para `Airbus/Helibras`;
- `Poços de Caldas` precisa de outra instância oficial da mesma família.

## Solução aplicada

- `alcoa_pocos_caldas_workday` entrou como nova profile curada `WORKDAY`;
- a strategy passou a resolver a facet `Brazil, MG, Poços de Caldas`;
- o normalizer ganhou board root e alias específicos da Alcoa.

## Validação

- testes automatizados:
  - `./mvnw -Dmaven.repo.local=/tmp/webscraper-m2 -Dtest=WorkdayJobNormalizerTest,WorkdayJobScraperStrategyTest,WorkdayJobImportUseCaseTest,TargetSiteOnboardingProfileCatalogTest,ImportingCrawlJobExecutionRunnerTest test`
- validação real:
  - `GET /actuator/health = UP`
  - `PROFILE_KEY=alcoa_pocos_caldas_workday KEEP_APP_RUNNING=true ./scripts/run-local-operational-check.sh`

## Lições aprendidas

- a família `WORKDAY` já está estável o suficiente para expansão por cidade sem duplicar o stack;
- o ponto crítico da expansão fica concentrado nas facets de localização e no board root.

## Estado final

- `alcoa_pocos_caldas_workday` implementada;
- trilha privada pronta para revisão de compliance/ativação.
