# STORY 2.5 — Baseline de persistência para `RawSnapshot`

## Objetivo

Materializar a base JPA mínima para armazenar snapshots HTTP brutos por `siteCode` e
`crawlExecutionId`, preparando auditoria e debugging offline sem ainda acoplar essa captura ao
runtime de scraping.

## Ciclo TDD

### Red

- criado teste de integração de repositório para exigir:
  - persistência de `RawSnapshotEntity` com `id`;
  - `crawlExecutionId` opcional;
  - consulta por `siteCode`;
  - consulta por `siteCode + responseStatus`.

### Green

- entidade `RawSnapshotEntity` implementada;
- repository `RawSnapshotRepository` implementado;
- migration Flyway criada para a tabela `raw_snapshots`;
- índices adicionados para `site_code`, `crawl_execution_id` e `fetched_at`.

### Refactor

- a migration foi publicada como `V010__create_raw_snapshots.sql`, não retroativamente como `V003`,
  para não quebrar ambientes já migrados;
- a fatia ficou explicitamente limitada à persistência, sem alegar captura automática no runtime.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/domain/model/RawSnapshotEntity.java`
- `src/main/java/com/campos/webscraper/domain/repository/RawSnapshotRepository.java`
- `src/main/resources/db/migration/V010__create_raw_snapshots.sql`
- `src/test/java/com/campos/webscraper/domain/repository/RawSnapshotRepositoryTest.java`
- `../../ADRs-web-scraping-vagas/ADR005-Persistence-and-JPA-Domain-Model-for-WebScraper.md`
- `../../ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`

## Problemas encontrados

- a primeira versão local da migration usava `V003`, mas o repositório já tinha sequência
  publicada até `V009`.

## Causa raiz

- a entidade `RawSnapshot` estava prevista conceitualmente nos ADRs desde o início, mas só foi
  materializada depois de várias migrations já consolidadas no histórico do projeto.

## Solução aplicada

- renumerar a migration para `V010`;
- atualizar a documentação para refletir a ordem real de entrega;
- manter a promessa funcional restrita à baseline de persistência.

## Lições aprendidas

- conceito antigo em ADR não autoriza inserir migration retroativa em projeto já evoluído;
- para features de observabilidade/auditoria, a documentação precisa diferenciar:
  - modelo de persistência pronto;
  - captura efetivamente ligada ao runtime.

## Estado final

- entidade e repository compilando;
- migration Flyway pronta como `V010`;
- testes focados de repositório previstos para validar a baseline;
- captura automática ainda não ligada ao pipeline operacional.
