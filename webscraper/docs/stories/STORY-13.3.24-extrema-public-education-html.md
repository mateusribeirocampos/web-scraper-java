# Story 13.3.24 — Extrema pública via portal de Educação + edital PDF

## Objetivo

Implementar a trilha pública oficial de `Extrema` a partir do portal da Secretaria de Educação,
descobrindo o edital canônico no detalhe do processo seletivo e persistindo o concurso municipal.

## Ciclo TDD

1. abrir parser/normalizer/strategy/import e cobertura do runner;
2. modelar a descoberta da página de detalhe e o filtro do edital canônico;
3. validar a trilha com `operational-check` real.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/ExtremaContestAttachment.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/ExtremaContestPreviewItem.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/ExtremaConcursosParser.java`
- `webscraper/src/main/java/com/campos/webscraper/application/enrichment/ExtremaContestPdfEnricher.java`
- `webscraper/src/main/java/com/campos/webscraper/application/normalizer/ExtremaContestNormalizer.java`
- `webscraper/src/main/java/com/campos/webscraper/application/strategy/ExtremaContestScraperStrategy.java`
- `webscraper/src/main/java/com/campos/webscraper/application/usecase/ExtremaContestImportUseCase.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `webscraper/src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java`
- `webscraper/src/main/resources/db/migration/V018__approve_municipal_extrema.sql`
- `webscraper/src/test/java/com/campos/webscraper/infrastructure/parser/ExtremaConcursosParserTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/normalizer/ExtremaContestNormalizerTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/strategy/ExtremaContestScraperStrategyTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/usecase/ExtremaContestImportUseCaseTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunnerTest.java`

## Problemas encontrados

- a listagem pública contém muitos anexos tardios, convocações e retificações no mesmo fluxo;
- a página de detalhe mistura o edital canônico com anexos de follow-up posteriores.

## Causa raiz

- o portal da Educação opera como publicação contínua do mesmo processo seletivo, acumulando
  republicações e convocações sob o mesmo contexto institucional.

## Solução aplicada

- a strategy passou a descobrir primeiro as páginas de detalhe da Educação;
- o parser seleciona o edital canônico e exclui anexos de seguimento;
- o enrichment PDF reaproveita a pilha municipal já estabilizada para cronograma e metadados.

## Validação

- testes automatizados:
  - `./mvnw -Dtest=ExtremaConcursosParserTest,ExtremaContestNormalizerTest,ExtremaContestScraperStrategyTest,ExtremaContestImportUseCaseTest,TargetSiteOnboardingProfileCatalogTest,ImportingCrawlJobExecutionRunnerTest test`
- validação real:
  - `GET /actuator/health = UP`
  - `POST /api/v1/onboarding-profiles/municipal_extrema/operational-check?smokeRun=true&daysBack=60`
  - resultado: `crawlExecutionId=128`, `status=SUCCEEDED`, `itemsFound=1`

## Lições aprendidas

- no portal de `Extrema`, a seleção do edital canônico precisa ser mais importante do que a
  simples presença de um PDF;
- o stack municipal HTML + enrichment PDF continua reutilizável para fechar cidades novas.

## Estado final

- `municipal_extrema` implementada;
- trilha pública validada em runtime real e pronta para ativação/compliance.
