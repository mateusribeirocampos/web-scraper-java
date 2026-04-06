# Story 13.3.8 — Santa Rita do Sapucaí Public Track Activation Review

## Status

Implemented and validated

## Objective

Fechar a ativação operacional/legal da trilha pública oficial da Câmara Municipal de Santa Rita do
Sapucaí, promovendo `camara_santa_rita_sapucai` para produção quando a evidência pública e o
runtime já estivessem suficientes.

## Review Inputs

- `robots.txt`: `https://www.santaritadosapucai.mg.leg.br/robots.txt`
- página oficial: `https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025`
- área institucional/transparência: `https://www.santaritadosapucai.mg.leg.br/transparencia`
- runtime real já validado:
  - `crawlExecutionId=116`
  - `SUCCEEDED`
  - `itemsFound=1`

## Decision

- a Câmara de Santa Rita pode avançar para produção;
- `camara_santa_rita_sapucai` passa a `APPROVED/enabled=true`;
- a trilha privada `lever_watchguard` continua separada e permanece `PENDING_REVIEW`.

## Applied Changes

- checklist curado da Câmara marcado como revisado e permissivo;
- perfil curado promovido para `APPROVED/enabled=true`;
- cobertura ajustada no catálogo e no validator para travar a promoção.

## Validation

Evidência pública revisada em `2026-04-06`:

- `robots.txt` respondeu com:
  - `User-agent: *`
  - `Disallow:`
- a página oficial de `Processos Seletivos 2025` permaneceu acessível no portal institucional;
- não foi encontrada restrição explícita adicional no portal institucional/transparência para essa
  coleta pública.

Validação técnica já fechada:

- `actuator/health = UP`
- `operational-check`:
  - `crawlExecutionId=116`
  - `SUCCEEDED`
  - `itemsFound=1`
- persistência confirmada em `public_contest_postings` para `target_site_id=26`

## Final State

- `camara_santa_rita_sapucai`: `APPROVED/enabled=true`
- `lever_watchguard`: `PENDING_REVIEW/enabled=false`
