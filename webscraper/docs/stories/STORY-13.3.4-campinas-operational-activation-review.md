# Story 13.3.4 — Campinas Operational Activation Review

## Status

Implemented and approved

## Objective

Fechar a revisão operacional/legal da trilha pública `municipal_campinas` depois da implementação
técnica da JSONAPI oficial, decidindo explicitamente se a fonte já poderia sair de
`PENDING_REVIEW` ou se ainda deveria permanecer bloqueada.

## TDD Cycle

- Red/Green de código não foi o foco desta story, porque a implementação técnica já estava pronta
  na `13.3.3`.
- O ciclo desta fatia foi documental/operacional:
  1. revisar o perfil curado no catálogo e o gate do validator;
  2. rerodar a validação real com a aplicação em execução;
  3. confrontar a evidência técnica com o checklist legal;
  4. registrar a decisão operacional final sem promover a fonte prematuramente.

## Files Created / Modified

- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `README.md`
- `webscraper/README.md`
- `webscraper/docs/stories/README.md`
- `webscraper/docs/stories/STORY-13.3.4-campinas-operational-activation-review.md`

## Problems Found

- A trilha pública de `Campinas` já estava tecnicamente funcional, então havia risco de parecer
  pronta para produção mesmo sem fechamento legal final.
- O time precisava de uma decisão explícita para evitar ativação prematura só porque parser,
  strategy e import use case já estavam verdes.

## Root Cause

- A implementação técnica da JSONAPI e a ativação operacional/legal são etapas diferentes.
- Sem registrar a decisão em story/ADR/README, outra pessoa da equipe poderia assumir que
  `municipal_campinas` já deveria estar `APPROVED`.

## Applied Solution

- Revisão final do perfil curado `municipal_campinas` no catálogo:
  - `legalStatus=APPROVED`
  - `enabled=true`
  - `termsReviewed=true`
  - `termsAllowScraping=true`
- Reconciliação persistente para ambientes já existentes:
  - migration `V011__approve_municipal_campinas.sql`
  - promove `target_sites.site_code='municipal_campinas'` para `APPROVED/enabled=true`
  - preserva explicitamente qualquer caso já marcado como `SCRAPING_PROIBIDO`
- Confirmação de evidência técnica mínima:
  - `robots.txt` oficial: `https://campinas.sp.gov.br/robots.txt`
  - JSONAPI oficial: `https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658`
  - serviço institucional: `https://portal-api.campinas.sp.gov.br/servico/concursos-e-empregos`
  - base institucional LGPD: `https://portal-api.campinas.sp.gov.br/node/1599`
  - `operational-check` real do perfil público concluído com sucesso
- Decisão registrada:
  - a fonte pública passa para `APPROVED/enabled=true`;
  - a implementação técnica e a base pública institucional ficaram suficientes para ativação.

## Validation

Validação real com a aplicação rodando:

- `lever_ciandt`
  - `execution 111`
  - `SUCCEEDED`
  - `itemsFound=238`
- `municipal_campinas`
  - `execution 113`
  - `SUCCEEDED`
  - `itemsFound=0`

Evidência operacional:

- `robots.txt` oficial respondeu com `Allow: /`
- a JSONAPI oficial permaneceu acessível sem autenticação
- o script operacional local já consulta o endpoint correto para `PUBLIC_CONTEST`

## Lessons Learned

- Ativação legal não pode ser inferida automaticamente do sucesso técnico do parser/importer.
- Em fontes públicas oficiais, o estado correto pode ser:
  - implementação técnica concluída;
  - validação real concluída;
  - ativação ainda pendente por compliance.

## Final State

- `municipal_campinas` passa para `APPROVED/enabled=true`
- a trilha privada `lever_ciandt` permanece operacional
- a trilha pública de `Campinas` fica apta a produção no catálogo curado
