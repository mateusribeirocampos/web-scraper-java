# Story 13.3.3 — Campinas Public Official JSONAPI

## Status

Implemented and ready for review

## Context

O portal público de `Campinas` para concursos não expõe uma listagem estática simples no HTML do frontend Angular. A fonte oficial estável encontrada foi o node JSONAPI do próprio portal municipal:

- `https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658`

Esse node representa o site `Concursos Públicos e Processos Seletivos` e hoje publica pelo menos um alerta oficial outbound para a banca/edital.

## Decision

Abrir a trilha pública de `Campinas` como fonte oficial `PUBLIC_CONTEST` baseada em JSONAPI, com um corte mínimo correto:

1. consumir o node oficial do portal;
2. extrair o alerta oficial ativo;
3. normalizar o alerta como posting canônico;
4. ignorar alertas expirados;
5. manter a ativação operacional separada da implementação técnica.

## Implementation Snapshot

- `municipal_campinas` entrou no catálogo curado
- `siteType=TYPE_E`, `extractionMode=API`, `jobCategory=PUBLIC_CONTEST`
- parser novo:
  - `CampinasConcursosParser`
- normalizer novo:
  - `CampinasContestNormalizer`
- strategy nova:
  - `CampinasContestScraperStrategy`
- import use case novo:
  - `CampinasContestImportUseCase`
- runner/onboarding sincronizados

## Current Behavior

- alerta oficial ativo vira `PublicContestPostingEntity`
- `canonicalUrl` e `editalUrl` apontam para o link oficial outbound do alerta
- `payloadJson` preserva a origem oficial (`officialSiteUrl` e `sourceApiUrl`)
- alerta expirado não é importado

## Exit Criteria

- implementação técnica pronta;
- testes focados verdes;
- compile verde;
- próxima etapa separada:
  - ativação operacional/legal do `municipal_campinas`;
  - ou aprofundamento do hub oficial para páginas-filhas adicionais além do alerta.
