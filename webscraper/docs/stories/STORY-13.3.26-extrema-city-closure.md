# Story 13.3.26 — Extrema: fechamento operacional/legal da cidade

## Objetivo

Fechar a revisão operacional/legal das duas trilhas de `Extrema` e encerrar a última cidade do
backlog híbrido municipal.

## Ciclo TDD

1. promover as profiles curadas para `APPROVED/enabled=true`;
2. adicionar migrations de bootstrap/reconciliação para ambientes já existentes;
3. validar catálogo, runner e runtime final da cidade.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/GupyBoardOnboardingProfiles.java`
- `webscraper/src/main/resources/db/migration/V018__approve_municipal_extrema.sql`
- `webscraper/src/main/resources/db/migration/V019__approve_gupy_specialdog_extrema.sql`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `README.md`
- `webscraper/README.md`
- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `webscraper/docs/stories/README.md`
- `webscraper/docs/stories/STORY-13.3.10-hybrid-cities-closure-plan.md`

## Problemas encontrados

- a validação real local ficou bloqueada por `checksum mismatch` antigo da migration `V017` no
  banco de desenvolvimento;
- era preciso confirmar que as duas trilhas realmente persistiam dados antes de declarar a cidade
  como fechada.

## Causa raiz

- o banco local já tinha uma versão anterior da `V017` aplicada;
- o backlog híbrido agora depende de validação real, não só de suíte focada.

## Solução aplicada

- `municipal_extrema` promovida para `APPROVED/enabled=true`;
- `gupy_specialdog_extrema` promovida para `APPROVED/enabled=true`;
- migrations `V018` e `V019` adicionadas para inserir/reconciliar `target_sites` e `crawl_jobs`;
- a validação operacional foi executada nas duas trilhas com a aplicação local em execução.

## Validação

- evidências revisadas:
  - `https://www.extrema.mg.gov.br/robots.txt`
  - `https://www.extrema.mg.gov.br/secretarias/educacao`
  - `https://www.extrema.mg.gov.br/politica-de-privacidade`
  - `https://specialdogcompany.gupy.io/robots.txt`
  - `https://specialdogcompany.gupy.io`
  - `https://www.specialdog.com.br/privacidade`
- testes automatizados:
  - `./mvnw -Dtest=ExtremaConcursosParserTest,ExtremaContestNormalizerTest,ExtremaContestScraperStrategyTest,ExtremaContestImportUseCaseTest,GupyJobBoardClientTest,TargetSiteOnboardingProfileCatalogTest,GetTargetSiteOnboardingProfileUseCaseTest,ImportingCrawlJobExecutionRunnerTest test`
- validação real:
  - `GET /actuator/health = UP`
  - `POST /api/v1/onboarding-profiles/municipal_extrema/operational-check?smokeRun=true&daysBack=60`
    - `siteId=31`
    - `crawlExecutionId=128`
    - `status=SUCCEEDED`
    - `itemsFound=1`
  - `POST /api/v1/onboarding-profiles/gupy_specialdog_extrema/operational-check?smokeRun=true&daysBack=60`
    - `siteId=32`
    - `crawlExecutionId=129`
    - `status=SUCCEEDED`
    - `itemsFound=1`

## Lições aprendidas

- a última cidade do backlog híbrido fechou sem exigir uma nova família ATS além das já abertas;
- o gargalo final passou a ser mais de validação operacional e reconciliação persistente do que de
  design de arquitetura.

## Estado final

- `municipal_extrema` promovida para `APPROVED/enabled=true`;
- `gupy_specialdog_extrema` promovida para `APPROVED/enabled=true`;
- `Extrema` fechada como a quinta cidade híbrida do backlog;
- backlog híbrido municipal encerrado.
