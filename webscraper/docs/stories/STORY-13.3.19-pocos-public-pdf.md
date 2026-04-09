# Story 13.3.19 — Poços de Caldas pública via edital PDF oficial

## Objetivo

Implementar a trilha pública oficial de `Poços de Caldas` usando o edital PDF municipal como fonte
canônica do processo seletivo.

## Ciclo TDD

1. abrir testes para parser PDF, normalizer, strategy, import use case, catálogo e runner;
2. implementar a família `municipal_pocos_caldas` com identidade estável do concurso;
3. ajustar o wiring até a suíte focada ficar verde.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/PocosCaldasContestPreviewItem.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/PocosCaldasContestPdfParser.java`
- `webscraper/src/main/java/com/campos/webscraper/application/normalizer/PocosCaldasContestNormalizer.java`
- `webscraper/src/main/java/com/campos/webscraper/application/strategy/PocosCaldasContestScraperStrategy.java`
- `webscraper/src/main/java/com/campos/webscraper/application/usecase/PocosCaldasContestImportUseCase.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `webscraper/src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java`
- `webscraper/src/test/java/com/campos/webscraper/infrastructure/parser/PocosCaldasContestPdfParserTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/normalizer/PocosCaldasContestNormalizerTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/strategy/PocosCaldasContestScraperStrategyTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/usecase/PocosCaldasContestImportUseCaseTest.java`

## Problemas encontrados

- a notícia HTML da prefeitura não era um ponto confiável de ingestão;
- o cronograma útil estava no PDF, então a estratégia precisava operar direto sobre o edital.

## Causa raiz

- o portal não expõe HTML estável o suficiente para a trilha pública desta cidade;
- o edital PDF é o artefato oficial com o cronograma realmente executável.

## Solução aplicada

- `municipal_pocos_caldas` entrou como `MUNICIPAL_PDF`, `STATIC_HTML`, `PUBLIC_CONTEST`;
- o parser lê número do edital e janela de inscrição direto do PDF oficial;
- o normalizer publica um único `PublicContestPostingEntity` canônico por edital.

## Validação

- testes automatizados:
  - `./mvnw -Dmaven.repo.local=/tmp/webscraper-m2 -Dtest=PocosCaldasContestPdfParserTest,PocosCaldasContestNormalizerTest,PocosCaldasContestScraperStrategyTest,PocosCaldasContestImportUseCaseTest,TargetSiteOnboardingProfileCatalogTest,ImportingCrawlJobExecutionRunnerTest test`
- validação real:
  - `GET /actuator/health = UP`
  - `PROFILE_KEY=municipal_pocos_caldas KEEP_APP_RUNNING=true ./scripts/run-local-operational-check.sh`

## Lições aprendidas

- nem toda trilha municipal precisa de HTML vivo; em alguns casos o PDF oficial é a superfície mais
  estável e auditável.

## Estado final

- `municipal_pocos_caldas` implementada;
- fonte pronta para revisão de compliance/ativação.
