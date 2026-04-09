# Story 13.3.20 — Poços de Caldas pública: ativação operacional/legal

## Objetivo

Fechar a revisão de compliance da trilha pública `municipal_pocos_caldas` e decidir se a fonte pode
ser promovida agora ou deve permanecer pendente.

## Ciclo TDD

1. revisar a base pública real e a vigência do edital usado como fonte canônica;
2. ajustar catálogo para refletir a decisão final da trilha pública;
3. validar a decisão com a aplicação rodando.

## Arquivos criados / modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidatorTest.java`

## Problemas encontrados

- o edital oficial encontrado é o `001/2025`, já encerrado, então a ativação em produção geraria
  apenas concurso fechado.

## Causa raiz

- a fonte é tecnicamente suportada e auditável, mas não existe ainda evidência de edital vigente
  para justificar `APPROVED/enabled=true` em `2026-04-09`.

## Solução aplicada

- `municipal_pocos_caldas` mantida em `PENDING_REVIEW/enabled=false`;
- a profile pública permanece ancorada na listagem oficial
  `https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609`, para permitir descoberta futura
  do edital canônico;
- o PDF oficial `001/2025` permanece como evidência pública auditada da rodada;
- o checklist curado permanece intencionalmente incompleto para ativação automática, até existir
  edital vigente revalidado;
- a trilha pública segue pronta para importação técnica, mas sem ativação até surgir edital vigente.

## Validação

- evidências revisadas:
  - `https://pocosdecaldas.mg.gov.br/robots.txt`
  - `https://pocosdecaldas.mg.gov.br/lgpd-lei-geral-de-protecao-de-dados/`
  - `https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609`
  - `https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf`
- testes automatizados:
  - `./mvnw -Dmaven.repo.local=/tmp/webscraper-m2 -Dtest=TargetSiteOnboardingProfileCatalogTest,TargetSiteOnboardingValidatorTest test`

## Lições aprendidas

- para fonte pública municipal baseada em PDF oficial, `robots + LGPD + artefato oficial` não
  bastam sozinhos: a vigência do edital precisa ser compatível com a ativação da fonte.

## Estado final

- `municipal_pocos_caldas` mantida em `PENDING_REVIEW/enabled=false`;
- metade pública de `Poços de Caldas` segue tecnicamente pronta, mas ainda não fechada.
