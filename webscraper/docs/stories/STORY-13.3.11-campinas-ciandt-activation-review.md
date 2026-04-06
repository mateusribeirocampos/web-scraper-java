# Story 13.3.11 — Campinas Private Track Activation Review for CI&T Lever

## Status

Implemented and approved

## Objective

Fechar a revisão de compliance/ativação da trilha privada `lever_ciandt`, para que `Campinas`
deixe de ter gaps pendentes no backlog híbrido.

## Context

Depois da promoção da trilha pública `municipal_campinas`, o único gap restante para encerrar
`Campinas` como cidade híbrida completa passou a ser a trilha privada da `CI&T` via `Lever`.

Essa trilha já estava:

- implementada;
- validada tecnicamente em runtime real;
- funcionando com o pipeline genérico `Lever`.

Faltava a decisão final de compliance.

## Applied Review

- revisão de `https://jobs.lever.co/robots.txt`
- revisão da API pública `https://api.lever.co/v0/postings/ciandt?mode=json`
- revisão da política pública de privacidade da CI&T:
  - `https://ciandt.com/br/pt-br/politica-de-privacidade`

## Decision

- `lever_ciandt` passa para `APPROVED/enabled=true`
- a URL legal curada fica apontando para a política pública de privacidade da CI&T
- a promoção também ganha reconciliação persistente para ambientes já bootstrapados

## Files Created / Modified

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/LeverBoardOnboardingProfiles.java`
- `webscraper/src/main/resources/db/migration/V012__approve_lever_ciandt.sql`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidatorTest.java`
- `webscraper/docs/stories/STORY-13.3.11-campinas-ciandt-activation-review.md`

## Final State

- `municipal_campinas`: `APPROVED/enabled=true`
- `lever_ciandt`: `APPROVED/enabled=true`
- `Campinas` passa a ser a primeira cidade do backlog híbrido completamente fechada
