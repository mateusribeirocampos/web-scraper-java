# Story 13.3.17 — Itajubá privada: ativação operacional/legal da trilha Airbus Workday

## Objetivo

Fechar a revisão de compliance da trilha privada `airbus_helibras_workday` e decidir se a fonte
pode ser promovida para `APPROVED/enabled=true`, encerrando `Itajubá` como cidade híbrida.

## Ciclo TDD

1. os testes do catálogo e do bootstrap foram ajustados primeiro para refletir a promoção curada;
2. uma migration de reconciliação foi adicionada para ambientes já bootstrapados;
3. a correção do `boardToken` de `WORKDAY` ganhou teste dedicado para evitar regressão no endpoint
   de onboarding.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/WorkdayBoardOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/GetTargetSiteOnboardingProfileUseCase.java`
- `webscraper/src/main/resources/db/migration/V015__approve_airbus_helibras_workday.sql`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/GetTargetSiteOnboardingProfileUseCaseTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/BootstrapTargetSiteFromProfileUseCaseTest.java`

## Problemas encontrados

- o runtime mostrava `boardToken=null` no perfil `WORKDAY`;
- a trilha privada precisava de reconciliação persistente para instâncias onde o `target_site`
  já existia como `PENDING_REVIEW/enabled=false`.

## Causa raiz

- o response mapper do onboarding ainda não tratava a nova família `WORKDAY`;
- a simples promoção no template curado não atualiza rows já persistidas.

## Solução aplicada

- `WORKDAY` passou a expor `boardToken` no endpoint de onboarding;
- a profile `airbus_helibras_workday` foi promovida para `APPROVED/enabled=true`;
- a migration `V015__approve_airbus_helibras_workday.sql` reconcilia ambientes existentes,
  preservando casos eventualmente marcados como `SCRAPING_PROIBIDO`.

## Validação

- testes automatizados:
  - `./mvnw -Dtest=GetTargetSiteOnboardingProfileUseCaseTest,TargetSiteOnboardingProfileCatalogTest,BootstrapTargetSiteFromProfileUseCaseTest,TargetSiteOnboardingValidatorTest,WorkdayJobNormalizerTest,WorkdayJobScraperStrategyTest,WorkdayJobImportUseCaseTest,TargetSiteOnboardingProfileControllerTest,ImportingCrawlJobExecutionRunnerTest test`
- evidência operacional/legal revisada:
  - `https://ag.wd3.myworkdayjobs.com/robots.txt`
  - `https://www.airbus.com/en/careers`
  - `https://www.airbus.com/en/privacy-notice`
- validação real:
  - `GET /api/v1/onboarding-profiles/airbus_helibras_workday`
  - `PROFILE_KEY=airbus_helibras_workday KEEP_APP_RUNNING=true ./scripts/run-local-operational-check.sh`

## Lições aprendidas

- para `API_OFICIAL`, a evidência legal pode ficar ancorada no conjunto `robots + careers oficial +
  política de privacidade`, mesmo sem uma página separada de ToS específica do board;
- qualquer nova família de onboarding precisa de teste real do DTO exposto, não só de wiring.

## Estado final

- `airbus_helibras_workday` promovida para `APPROVED/enabled=true`;
- `Itajubá` fica apta a ser registrada como a terceira cidade híbrida encerrada, após a validação
  final de runtime desta fatia.
