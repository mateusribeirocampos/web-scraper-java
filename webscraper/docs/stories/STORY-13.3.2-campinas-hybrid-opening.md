# Story 13.3.2 — Campinas Hybrid Opening

## Status

Private track implemented; public official track implemented with pending operational activation

## Context

`Campinas` foi priorizada como a primeira cidade do backlog híbrido por combinar:

- ecossistema privado forte de tecnologia;
- chance real de vagas locais em ATS públicos;
- presença de setor público suficientemente grande para concursos e processos seletivos;
- retorno potencial maior do que abrir mais uma prefeitura isolada sem trilha privada clara.

## Decision

A abertura de `Campinas` parte de duas trilhas concretas:

1. privada:
   - `CI&T` via `Lever` público
   - referência: `https://jobs.lever.co/ciandt`
2. pública:
   - portal oficial `Concursos e Empregos` da Prefeitura de Campinas
   - referência operacional: portal oficial da prefeitura

## Recommended Execution Order

1. concluir review da trilha privada da `CI&T` via `Lever`
2. validar onboarding público do portal de concursos da prefeitura
3. escolher a próxima fatia executável:
   - pública;
   - ou mista

## Implementation Snapshot

- `lever_ciandt` entrou no catálogo curado como fonte `LEVER`
- onboarding privado foi registrado para `CI&T Careers via Lever`
- strategy, normalizer, import use case e runner foram implementados
- testes unitários focados ficaram verdes
- validação de integração com Testcontainers ficou pendente do ambiente local de Docker
- `municipal_campinas` entrou como fonte `PUBLIC_CONTEST` oficial da prefeitura
- a trilha pública usa o JSONAPI oficial do portal Campinas filtrado pelo node `113658`
- o parser atual modela o alerta oficial ativo como posting canônico e ignora alertas expirados
- a trilha pública ficou `PENDING_REVIEW/enabled=false` até a ativação operacional final

## Why This Order

- a trilha privada via `Lever` tende a ser API-first e mais barata para abrir;
- a trilha pública oficial fecha o lado `PUBLIC_CONTEST` da cidade sem depender de agregadores;
- a combinação permite que `Campinas` vire a primeira cidade explicitamente híbrida do backlog.

## Exit Criteria

- trilha privada `Lever` da `CI&T` implementada;
- trilha pública oficial de `Campinas` implementada;
- ativação operacional/legal da trilha pública ainda pendente;
- próxima story pronta para ativar operacionalmente `municipal_campinas` ou aprofundar a descoberta de páginas-filhas/menus do hub oficial.
