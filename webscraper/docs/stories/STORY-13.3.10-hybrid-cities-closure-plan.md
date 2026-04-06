# Story 13.3.10 — Hybrid Cities Closure Plan

## Status

Ready for implementation planning

## Objective

Ordenar explicitamente as próximas tasks do backlog híbrido para fechar todas as cidades abertas ou
priorizadas, começando pela cidade com menos gaps remanescentes e terminando na cidade com mais
lacunas de descoberta/implementação.

## Context

Depois das entregas de `Campinas` e `Santa Rita do Sapucaí`, o backlog híbrido já não está mais em
fase de descoberta ampla. O próximo passo correto é fechar cada cidade de ponta a ponta, sem abrir
novas frentes em paralelo desnecessariamente.

O critério definido para a ordem de ataque passa a ser:

1. cidade com menos gaps restantes;
2. cidade com mais evidência técnica já validada;
3. cidade com menor risco de espalhar o backlog antes do fechamento das anteriores.

## Ordered Closure Tasks

### 1. Campinas

Cidade mais próxima de encerramento.

Gaps remanescentes:

- nenhum após a `13.3.11`.

Definition of done da cidade:

- `municipal_campinas` já segue `APPROVED/enabled=true`;
- `lever_ciandt` segue `APPROVED/enabled=true`;
- cidade encerrada no backlog híbrido.

### 2. Santa Rita do Sapucaí

Cidade parcialmente fechada, com trilha pública resolvida e trilha privada pendente.

Gaps remanescentes:

- revisar compliance da trilha privada `lever_watchguard`;
- decidir se `lever_watchguard` pode sair de `PENDING_REVIEW`;
- alinhar documentação final da cidade após a decisão da trilha privada.

Definition of done da cidade:

- `camara_santa_rita_sapucai` já segue `APPROVED/enabled=true`;
- `lever_watchguard` com decisão final explícita:
  - promovida para `APPROVED/enabled=true`; ou
  - bloqueada com motivo definitivo documentado;
- cidade marcada como encerrada no backlog híbrido.

### 3. Itajubá

Cidade aberta documentalmente, ainda sem execução.

Gaps remanescentes:

- mapear uma trilha privada concreta;
- mapear uma trilha pública oficial concreta;
- escolher a primeira implementação executável;
- implementar a primeira trilha;
- validar runtime real;
- revisar compliance/ativação;
- implementar a segunda trilha;
- validar runtime real;
- revisar compliance/ativação;
- encerrar a cidade no backlog.

### 4. Poços de Caldas

Cidade priorizada, mas ainda não aberta em execução.

Gaps remanescentes:

- abrir a story de entrada da cidade;
- mapear trilha privada;
- mapear trilha pública;
- escolher a primeira implementação executável;
- implementar as trilhas necessárias;
- validar runtime real;
- revisar compliance/ativação;
- encerrar a cidade no backlog.

### 5. Extrema

Cidade com maior número de lacunas no momento.

Gaps remanescentes:

- abrir a story de entrada da cidade;
- mapear trilha privada;
- mapear trilha pública;
- escolher a primeira implementação executável;
- implementar as trilhas necessárias;
- validar runtime real;
- revisar compliance/ativação;
- encerrar a cidade no backlog.

## Execution Rule

- nenhuma cidade nova deve começar antes da cidade anterior ter sua decisão final registrada;
- `Campinas` fica encerrada como a primeira cidade híbrida completa;
- o foco imediato passa a ser `Santa Rita do Sapucaí`;
- `Itajubá`, `Poços de Caldas` e `Extrema` só avançam quando as cidades anteriores estiverem
  documental e operacionalmente resolvidas.

## Exit Criteria

- ordem oficial de fechamento das cidades registrada;
- gaps por cidade explicitados;
- próxima task imediatamente executável definida sem ambiguidade.
