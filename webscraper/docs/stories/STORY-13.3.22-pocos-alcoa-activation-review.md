# Story 13.3.22 — Poços de Caldas privada: ativação operacional/legal

## Objetivo

Fechar a revisão de compliance da trilha privada `alcoa_pocos_caldas_workday` e decidir a
promoção para `APPROVED/enabled=true`, deixando a cidade dependente apenas da trilha pública.

## Ciclo TDD

1. ajustar catálogo/validator para refletir a promoção curada;
2. adicionar migration de reconciliação para ambientes já bootstrapados;
3. validar a decisão com o runtime da aplicação.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/WorkdayBoardOnboardingProfiles.java`
- `webscraper/src/main/resources/db/migration/V017__approve_alcoa_pocos_caldas_workday.sql`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidatorTest.java`

## Problemas encontrados

- a promoção precisava alcançar também sites já persistidos em `target_sites`.

## Causa raiz

- a alteração de compliance na profile curada não atualiza sozinha ambientes já bootstrapados.

## Solução aplicada

- `alcoa_pocos_caldas_workday` promovida para `APPROVED/enabled=true`;
- migration `V017__approve_alcoa_pocos_caldas_workday.sql` adicionada para reconciliação.

## Validação

- evidências revisadas:
  - `https://alcoa.wd5.myworkdayjobs.com/robots.txt`
  - `https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs`
  - `https://www.alcoa.com/global/en/general/privacy`
- testes automatizados:
  - `./mvnw -Dmaven.repo.local=/tmp/webscraper-m2 -Dtest=TargetSiteOnboardingProfileCatalogTest,TargetSiteOnboardingValidatorTest test`

## Lições aprendidas

- para `Workday` oficial, a combinação `robots + board oficial + política pública de privacidade`
  continua suficiente como base auditável de ativação.

## Estado final

- `alcoa_pocos_caldas_workday` promovida para `APPROVED/enabled=true`;
- `Poços de Caldas` fica com a trilha privada fechada, mas continua pendente na trilha pública.
