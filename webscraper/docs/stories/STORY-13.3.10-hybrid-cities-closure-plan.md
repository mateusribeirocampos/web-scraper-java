# Story 13.3.10 — Hybrid Cities Closure Plan

## Status

Completed

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

Cidade encerrada com trilha pública aprovada e trilha privada bloqueada definitivamente.

Gaps remanescentes:

- nenhum após a `13.3.12`.

Definition of done da cidade:

- `camara_santa_rita_sapucai` já segue `APPROVED/enabled=true`;
- `lever_watchguard` segue `SCRAPING_PROIBIDO/enabled=false` com blocker definitivo documentado;
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

Cidade final do backlog híbrido, agora fechada com duas trilhas oficiais executáveis.

Gaps remanescentes:

- nenhum após as `13.3.23` a `13.3.26`.

## Execution Rule

- nenhuma cidade nova deve começar antes da cidade anterior ter sua decisão final registrada;
- `Campinas` ficou encerrada como a primeira cidade híbrida completa;
- `Santa Rita do Sapucaí`, `Itajubá` e `Extrema` também foram fechadas dentro da ordem definida;
- `Poços de Caldas` ficou fechada com trilha privada aprovada e trilha pública conscientemente
  retida até edital vigente.

## Exit Criteria

- ordem oficial de fechamento das cidades registrada;
- gaps por cidade explicitados;
- backlog híbrido municipal encerrado ao final da última cidade.
