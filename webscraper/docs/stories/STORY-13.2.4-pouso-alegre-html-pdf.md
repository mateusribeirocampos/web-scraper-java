# Story 13.2.4 — Pouso Alegre HTML + PDF

## Objetivo

Adicionar `Pouso Alegre` como a segunda fonte municipal `PUBLIC_CONTEST`, reaproveitando o padrão
já amadurecido em `Inconfidentes`:

- listagem HTML oficial;
- detalhe por página estruturada;
- seleção do anexo principal do tipo `Edital`;
- enrichment de PDF para prazo, escolaridade e cargo;
- persistência normalizada como `PUBLIC_CONTEST`.

## Escopo desta fatia

- onboarding formal de `municipal_pouso_alegre`;
- parser da listagem `https://www.pousoalegre.mg.gov.br/concursos-publicos`;
- parser da página de detalhe `concursos_view/<id>`;
- strategy com fetch da listagem e depois dos detalhes;
- import use case dedicado;
- wiring no `ImportingCrawlJobExecutionRunner`;
- testes focados do parser, strategy, import, catálogo e runner.

## Decisões

- manter classes próprias de `Pouso Alegre` nesta fase para reduzir risco;
- reaproveitar o parser de metadata de PDF já consolidado;
- usar o portal de concursos como `canonicalUrl` público e o PDF de edital como `editalUrl`;
- escolher o anexo principal pelo tipo `Edital`, ignorando `Classificação`, `Convocação` e afins
  como identidade do concurso.

## Evidências operacionais

- `robots.txt`: `https://www.pousoalegre.mg.gov.br/robots.txt`
- Resultado observado em `2026-03-30`: `User-agent: *` + `Disallow:` vazio
- Política pública usada no checklist:
  `https://pousoalegre.mg.gov.br/politica_privacidade`

## Implementação nesta fatia

- `PousoAlegreConcursosParser`
- `PousoAlegreContestPdfEnricher`
- `PousoAlegreContestNormalizer`
- `PousoAlegreContestScraperStrategy`
- `PousoAlegreContestImportUseCase`
- profile curado `municipal_pouso_alegre`

## Testes

- `PousoAlegreConcursosParserTest`
- `PousoAlegreContestScraperStrategyTest`
- `PousoAlegreContestImportUseCaseTest`
- `TargetSiteOnboardingProfileCatalogTest`
- `ImportingCrawlJobExecutionRunnerTest`

## Próximo passo

Fechar a review da `13.2.4`, validar o fluxo real local e então decidir se o próximo município será
`Munhoz` ou se vale uma extração municipal comum para reduzir duplicação entre `Inconfidentes` e
`Pouso Alegre`.
