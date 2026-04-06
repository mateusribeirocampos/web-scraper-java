# Story 13.3.9 — Itajubá Hybrid Opening

## Status

Open

## Objective

Abrir `Itajubá` como a próxima cidade do backlog híbrido depois de `Santa Rita do Sapucaí`,
preparando uma trilha privada e uma trilha pública oficial para a próxima sequência de
implementações.

## Context

- `Santa Rita do Sapucaí` já teve:
  - trilha privada `lever_watchguard` implementada e validada tecnicamente, mas ainda
    `PENDING_REVIEW`;
  - trilha pública da Câmara implementada, validada e promovida para `APPROVED/enabled=true`.
- o backlog híbrido priorizado seguia com `Itajubá` logo depois de `Santa Rita do Sapucaí`.
- antes de avançar demais no backlog, ficou registrado também o próximo checkpoint de fazer um
  teste ponta a ponta com a aplicação cobrindo as trilhas já construídas.

## Decision

- `Itajubá` passa a ser a próxima cidade híbrida aberta;
- `lever_watchguard` não recebe nova ação de compliance nesta fatia;
- a próxima implementação da cidade ainda depende do mapeamento das duas trilhas:
  - privada;
  - pública oficial.

## Next Steps

1. revisar a task contra ADRs, stories, commits e `README.md`
2. mapear uma trilha privada real ligada ao polo tech de `Itajubá`
3. mapear uma trilha pública oficial de concursos/processos seletivos
4. escolher qual trilha sai primeiro na implementação
5. antes ou logo após essa abertura, executar um teste ponta a ponta com a aplicação para validar
   o processo completo já entregue

## Final State

- `Itajubá` fica aberta como a próxima cidade do backlog híbrido;
- `lever_watchguard` permanece `PENDING_REVIEW`;
- o projeto passa a carregar explicitamente a recomendação de um teste operacional ponta a ponta
  antes do próximo bloco grande de expansão.
