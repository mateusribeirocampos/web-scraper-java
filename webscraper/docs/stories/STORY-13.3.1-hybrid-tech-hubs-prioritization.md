# Story 13.3.1 — Priorização de Polos Tecnológicos com Backlog Híbrido

## Status

Ready for implementation planning

## Context

Após estabilizar a base municipal `PUBLIC_CONTEST` em `Inconfidentes`, `Pouso Alegre` e
`Munhoz`, a próxima expansão não deve olhar apenas prefeituras isoladas. O retorno esperado passa
por cidades que combinam:

- setor privado com empresas de tecnologia;
- setor público com concursos e processos seletivos;
- volume suficiente para justificar manutenção de integração no catálogo.

## Decision

Backlog híbrido priorizado:

1. `Campinas`
2. `Santa Rita do Sapucaí`
3. `Itajubá`
4. `Poços de Caldas`
5. `Extrema`

## Rationale

- `Campinas` tem o maior potencial de volume e diversidade entre privado e público.
- `Santa Rita do Sapucaí` e `Itajubá` têm ecossistema tech explícito e aderente ao foco do produto.
- `Poços de Caldas` e `Extrema` continuam relevantes, mas entram depois na fila de ataque.

## Scope For Next Story

- validar por cidade pelo menos uma trilha concreta de captura:
  - ATS público / API oficial / página de carreira própria;
  - prefeitura / autarquia / portal oficial de concursos;
- decidir se a próxima implementação abre pela trilha:
  - privada;
  - pública;
  - ou mista.

## Exit Criteria

- backlog híbrido documentado;
- ordem de implementação acordada;
- próxima cidade escolhida com hipótese técnica inicial clara.
