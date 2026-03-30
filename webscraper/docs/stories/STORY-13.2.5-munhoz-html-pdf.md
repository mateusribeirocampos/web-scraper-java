# Story 13.2.5 — Munhoz HTML + PDF

## Objetivo

Adicionar `municipal_munhoz` como terceira fonte municipal `PUBLIC_CONTEST`, reaproveitando o
pipeline estruturado de HTML + PDF já estabilizado em `Pouso Alegre`.

## Escopo

- onboarding formal do domínio
- parser da listagem `concursos-publicos`
- parser do detalhe `concursos_view/<id>`
- seleção do anexo canônico de edital mesmo quando vários anexos usam `Tipo=Edital`
- enrichment de PDF reaproveitando o parser já existente
- normalização `PUBLIC_CONTEST`
- wiring no runner e testes focados

## Notas Técnicas

- Em `Munhoz`, a tabela de anexos exige discriminar o edital principal pela descrição, não só pelo
  tipo do anexo.
- O perfil curado entra como `PENDING_REVIEW/enabled=false` até o fechamento formal de evidências
  legais/operacionais.

## Validação

- `./mvnw -q -Dtest=MunhozConcursosParserTest,MunhozContestScraperStrategyTest,MunhozContestImportUseCaseTest,TargetSiteOnboardingProfileCatalogTest,ImportingCrawlJobExecutionRunnerTest test`
- `./mvnw -Dmaven.repo.local=/tmp/webscraper-m2 -DskipTests compile`
