# Story 13.3.14 — Itajubá pública via Câmara HTML

## Objetivo

Implementar a primeira trilha executável de `Itajubá` pela fonte pública oficial da Câmara
Municipal, usando a página institucional de lançamento do concurso e o PDF oficial do edital.

## Ciclo TDD

1. escrever fixture mínima da página de lançamento do concurso da Câmara de Itajubá;
2. abrir testes de parser, normalizer, strategy, import use case, catálogo e runner;
3. implementar o parser HTML estático e a normalização canônica do concurso;
4. ajustar o wiring de onboarding e execução até a suíte focada ficar verde.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/CamaraItajubaContestAttachment.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/CamaraItajubaContestPreviewItem.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/CamaraItajubaContestPageParser.java`
- `webscraper/src/main/java/com/campos/webscraper/application/normalizer/CamaraItajubaContestNormalizer.java`
- `webscraper/src/main/java/com/campos/webscraper/application/strategy/CamaraItajubaContestScraperStrategy.java`
- `webscraper/src/main/java/com/campos/webscraper/application/usecase/CamaraItajubaContestImportUseCase.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `webscraper/src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java`
- `webscraper/src/test/resources/fixtures/camara-itajuba/camara-itajuba-launch-page.html`
- `webscraper/src/test/java/com/campos/webscraper/infrastructure/parser/CamaraItajubaContestPageParserTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/normalizer/CamaraItajubaContestNormalizerTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/strategy/CamaraItajubaContestScraperStrategyTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/usecase/CamaraItajubaContestImportUseCaseTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunnerTest.java`

## Problemas encontrados

- a hipótese inicial de usar a prefeitura como primeira trilha pública da cidade ficou fraca por
  desafio anti-bot no domínio institucional;
- a primeira URL escolhida para a cidade não expunha o edital de forma consistente no HTML vivo;
- a página de lançamento do concurso é a fonte estável que realmente publica o edital oficial em
  PDF no corpo do conteúdo institucional;
- o nome do arquivo do edital embute `2023`, mas o cronograma real da inscrição/provas está em
  `2024`, então a heurística de datas não podia usar apenas o nome do PDF.

## Causa raiz

- o concurso da Câmara de Itajubá mistura página institucional viva com anexo PDF do edital, e o
  cronograma não está modelado em tabela estruturada;
- a informação oficial está espalhada entre título da página, metadados `article:published_time`,
  texto corrido e links PDF.

## Solução aplicada

- a fonte `camara_itajuba` entrou como `LEGISLATIVE_HTML`, `STATIC_HTML`,
  `PUBLIC_CONTEST`, inicialmente em `PENDING_REVIEW/enabled=false`;
- o parser da primeira fatia lê:
  - título do concurso;
  - data oficial da página;
  - número de vagas;
  - descrição salarial;
  - data final de inscrição;
  - data de prova;
  - link canônico do edital PDF;
  - anexos PDF complementares;
- o normalizer converte isso em um único `PublicContestPostingEntity` canônico usando o PDF do
  edital como `canonicalUrl` e base de identidade estável.

## Validação

- testes automatizados:
  - `./mvnw -Dtest=CamaraItajubaContestPageParserTest,CamaraItajubaContestNormalizerTest,CamaraItajubaContestScraperStrategyTest,CamaraItajubaContestImportUseCaseTest,TargetSiteOnboardingProfileCatalogTest,ImportingCrawlJobExecutionRunnerTest test`
- validação real:
  - `GET /actuator/health = UP`
  - `PROFILE_KEY=camara_itajuba JOB_POSTINGS_CATEGORY=PUBLIC_CONTEST PUBLIC_CONTEST_STATUS=OPEN PUBLIC_CONTEST_ORDER_BY=registrationEndDate ./scripts/run-local-operational-check.sh`
  - `crawlExecutionId=121`
  - `status=SUCCEEDED`
  - `itemsFound=1`
  - persistência confirmada no endpoint `/api/v1/public-contests?status=REGISTRATION_CLOSED&orderBy=registrationEndDate`, retornando a entrada canônica da Câmara de Itajubá com `publishedAt=2023-12-13`

## Lições aprendidas

- em páginas WordPress institucionais, um fixture mínimo do bloco de conteúdo é suficiente para
  proteger o parser sem carregar todo o layout do portal;
- para concursos oficiais, o PDF do edital continua sendo a melhor URL canônica, mesmo quando a
  página institucional é o ponto de entrada operacional.

## Estado final

- `camara_itajuba` implementada em código;
- suíte focada verde;
- validação real concluída com `itemsFound=1`;
- fonte pronta para a revisão de compliance da próxima fatia.
