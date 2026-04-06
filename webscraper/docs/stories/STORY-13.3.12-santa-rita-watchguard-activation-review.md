# Story 13.3.12 — Santa Rita do Sapucaí Private Track Activation Review

## Status

Implemented with definitive blocker

## Objective

Fechar a revisão operacional/legal da trilha privada `lever_watchguard`, decidindo explicitamente se
ela pode entrar em produção ou se precisa ser encerrada com bloqueio definitivo de compliance.

## Review Inputs

- `robots.txt`: `https://jobs.lever.co/robots.txt`
- jobs API pública: `https://api.lever.co/v0/postings/watchguard?mode=json`
- Terms of Use da WatchGuard: `https://www.watchguard.com/wgrd-trust-center/terms-of-use`
- Privacy Policy da WatchGuard: `https://www.watchguard.com/wgrd-trust-center/privacy-policy`
- runtime técnico já validado anteriormente:
  - `crawlExecutionId=114`
  - `SUCCEEDED`
  - `itemsFound=31`

## Decision

- `lever_watchguard` não pode avançar para produção;
- o perfil passa a `SCRAPING_PROIBIDO/enabled=false`;
- `Santa Rita do Sapucaí` fica encerrada como cidade híbrida com:
  - trilha pública oficial aprovada;
  - trilha privada bloqueada definitivamente por compliance.

## Why It Was Blocked

Evidência revisada em `2026-04-06`:

- `jobs.lever.co/robots.txt` continua permitindo leitura para fins de busca;
- porém os `Terms of Use` da WatchGuard dizem que os “Services” incluem sites ou serviços
  relacionados em que os termos estejam vinculados ou referenciados;
- esses termos proíbem explicitamente:
  - `data mining`
  - `robots`
  - métodos de extração de dados desenhados para scraping
  - aplicações que interajam com os serviços sem autorização prévia por escrito

Com isso, a trilha técnica continua funcionando, mas a base legal não é forte o suficiente para
produção e passa a ser tratada como bloqueio definitivo.

## Applied Changes

- perfil curado `lever_watchguard` reclassificado para `SCRAPING_PROIBIDO/enabled=false`;
- checklist curado passa a apontar para os `Terms of Use` efetivamente revisados;
- reconciliação persistente adicionada para ambientes já bootstrapados;
- documentação da cidade atualizada para refletir a decisão final da trilha privada.

## Validation

- `./mvnw -Dtest=TargetSiteOnboardingProfileCatalogTest,TargetSiteOnboardingValidatorTest test`
- runtime local:
  - `GET /actuator/health = UP`
  - `GET /api/v1/onboarding-profiles/lever_watchguard` deve refletir `SCRAPING_PROIBIDO/enabled=false`

## Final State

- `camara_santa_rita_sapucai`: `APPROVED/enabled=true`
- `lever_watchguard`: `SCRAPING_PROIBIDO/enabled=false`
- `Santa Rita do Sapucaí` encerrada no backlog híbrido
