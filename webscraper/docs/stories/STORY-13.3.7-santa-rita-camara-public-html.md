# Story 13.3.7 — Santa Rita do Sapucaí Public Track via Câmara HTML

## Status

Implemented and ready for review

## Objective

Abrir a trilha pública oficial de `Santa Rita do Sapucaí` pela página da Câmara Municipal
`Processos Seletivos 2025`, modelando a página HTML como fonte principal e os PDFs oficiais como
anexos canônicos dos editais.

## TDD Cycle

1. Red:
   - adicionar testes de parser para os blocos de edital, cronograma e anexos oficiais;
   - adicionar testes de normalização, strategy, import use case, catálogo e runner.
2. Green:
   - implementar parser HTML estático da página oficial da Câmara;
   - normalizar os editais em `PublicContestPostingEntity`;
   - registrar a nova fonte curada e ligar o runner ao novo import use case.
3. Refactor:
   - manter o pipeline municipal isolado em classes próprias desta Câmara;
   - documentar que o HTML é a fonte primária de metadados e os PDFs seguem como evidência
     oficial/canonical URL.

## Files Created / Modified

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `webscraper/src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java`
- `webscraper/src/main/java/com/campos/webscraper/application/normalizer/CamaraSantaRitaContestNormalizer.java`
- `webscraper/src/main/java/com/campos/webscraper/application/strategy/CamaraSantaRitaContestScraperStrategy.java`
- `webscraper/src/main/java/com/campos/webscraper/application/usecase/CamaraSantaRitaContestImportUseCase.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/CamaraSantaRitaContestAttachment.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/CamaraSantaRitaContestPreviewItem.java`
- `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/CamaraSantaRitaProcessosSeletivosParser.java`
- `webscraper/src/test/java/com/campos/webscraper/application/normalizer/CamaraSantaRitaContestNormalizerTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/strategy/CamaraSantaRitaContestScraperStrategyTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/usecase/CamaraSantaRitaContestImportUseCaseTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunnerTest.java`
- `webscraper/src/test/java/com/campos/webscraper/infrastructure/parser/CamaraSantaRitaProcessosSeletivosParserTest.java`
- `webscraper/src/test/resources/fixtures/camara-santa-rita/camara-santa-rita-processos-seletivos-2025.html`

## Problems Found

- o conteúdo oficial é uma página HTML manual, sem JSON estruturado;
- cada edital aparece em um bloco com tabelas e links de cronograma;
- os PDFs oficiais convivem com anexos de follow-up como retificação, homologação e prorrogação.

## Root Cause

- a Câmara publica os processos seletivos em HTML estático com estrutura humana, não em API;
- o valor operacional vem de combinar:
  - título do edital;
  - cronograma em tabela;
  - link oficial do PDF do edital.

## Applied Solution

- a página `Processos Seletivos 2025` foi modelada como fonte `STATIC_HTML`;
- o parser identifica blocos por `Edital nº X/2025: ...`;
- o cronograma fornece:
  - data de publicação;
  - período de inscrições;
- o link `Divulgação do Edital` vira o `editalUrl` canônico;
- anexos de follow-up continuam registrados no payload, mas não podem substituir o edital
  principal;
- a fonte curada entrou como `camara_santa_rita_sapucai` e continua `PENDING_REVIEW/enabled=false`.

## Validation

Testes automatizados executados:

- `./mvnw -Dtest=CamaraSantaRitaProcessosSeletivosParserTest,CamaraSantaRitaContestNormalizerTest,CamaraSantaRitaContestScraperStrategyTest,CamaraSantaRitaContestImportUseCaseTest,TargetSiteOnboardingProfileCatalogTest,ImportingCrawlJobExecutionRunnerTest test`
- resultado esperado nesta fatia: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`

Validação real:

- deve ser fechada na sequência com a aplicação rodando via `operational-check` do novo perfil
  `camara_santa_rita_sapucai`

## Lessons Learned

- para fontes legislativas pequenas, HTML estático com PDFs oficiais pode ser suficiente sem abrir
  enrichment PDF cedo demais;
- usar o HTML como verdade de cronograma reduz custo de parsing quando a página já expõe metadados
  confiáveis.

## Final State

- `Santa Rita do Sapucaí` passa a ter também a trilha pública oficial implementada;
- a modelagem escolhida é HTML estático com anexos PDF oficiais;
- a ativação continua dependente de review legal/operacional posterior.
