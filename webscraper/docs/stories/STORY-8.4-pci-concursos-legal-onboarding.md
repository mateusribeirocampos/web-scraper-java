# STORY 8.4 — Verificar robots.txt e ToS do PCI Concursos antes de ativar em produção

**Status:** ✅ Concluída
**Iteration:** 8 — Primeiro scraper HTML estático: PCI Concursos
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 8.4

---

## Objetivo

Fechar o gate legal/operacional do PCI Concursos antes de qualquer ativação em produção:

- verificar `robots.txt`
- revisar a existência de termos públicos utilizáveis
- transformar a evidência em `legalStatus` e `enabled`
- bloquear ativação quando o checklist de onboarding estiver incompleto

---

## Ciclo TDD

### RED — teste de onboarding / metadata primeiro

Foi criado `TargetSiteOnboardingValidatorTest` cobrindo:

- PCI ficando `PENDING_REVIEW` quando não há documentação pública suficiente de ToS
- bloqueio explícito quando onboarding marca scraping como proibido
- aprovação apenas quando o checklist está completo e permissivo

O RED inicial falhou por compilação, porque ainda não existiam:

- `SiteOnboardingChecklist`
- `TargetSiteOnboardingValidator`
- `TargetSiteOnboardingDecision`

### GREEN — implementação mínima

Foi implementado:

1. `OnboardingLegalCategory`
2. `SiteOnboardingChecklist`
3. `TargetSiteOnboardingDecision`
4. `TargetSiteOnboardingValidator`

Regras entregues:

- checklist incompleto => `legalStatus = PENDING_REVIEW` e `enabled = false`
- robots/ToS proibitivos => `legalStatus = SCRAPING_PROIBIDO` e `enabled = false`
- checklist completo e permissivo => `legalStatus = APPROVED`
- um site só permanece `enabled = true` quando já estava habilitado e o onboarding fecha como aprovado
- regras de proibição de scraping agora só valem para categorias que realmente dependem de scraping
  HTML; fontes `API_OFICIAL` e `DADOS_PUBLICOS` não são reprovadas por `robots`/ToS de scraping
- quando existe proibição explícita revisada, `SCRAPING_PROIBIDO` tem precedência sobre checklist
  incompleto; ausência de documentação não mascara uma vedação real
- o validator agora reconcilia `legalCategory` com `siteType`/`extractionMode`
- aprovação final já devolve o `TargetSite` com `enabled = true`, pronto para a primeira ativação
- `DADOS_PUBLICOS` agora permanece isento das proibições específicas de scraping mesmo quando a
  extração técnica é HTML
- `API_OFICIAL` e `DADOS_PUBLICOS` agora não exigem URL dedicada de ToS para aprovação
- o mismatch entre categoria legal e metadata técnica agora é validado nos dois sentidos
- revisão de ToS continua obrigatória para qualquer aprovação, mas URL dedicada só é exigida nos
  caminhos `SCRAPING_*`
- `DADOS_PUBLICOS` em HTML volta a respeitar proibição explícita de acesso automatizado quando a
  evidência revisada nega scraping
- metadata API agora só é considerada coerente quando `siteType` e `extractionMode` apontam juntos
  para API
- negação explícita de termos revisados agora bloqueia também fontes API-backed
- metadata API inconsistente agora fica `PENDING_REVIEW` antes de qualquer proibição por `robots`
  de HTML
- mismatch entre checklist `API_OFICIAL` e metadata HTML coerente também fica `PENDING_REVIEW`,
  sem cair indevidamente em `SCRAPING_PROIBIDO`

### REFACTOR

O validator foi mantido isolado da persistência e do scheduler. A story precisava primeiro fechar a
política de produção, sem acoplar essa decisão a seed, migration ou controller.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/onboarding/OnboardingLegalCategory.java` | Criado | Categoria legal do checklist ADR002 |
| `src/main/java/com/campos/webscraper/application/onboarding/SiteOnboardingChecklist.java` | Criado | Evidência obrigatória para ativação em produção |
| `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingDecision.java` | Criado | Resultado da avaliação de onboarding |
| `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidator.java` | Criado | Regra de bloqueio/aprovação por checklist |
| `src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidatorTest.java` | Criado | RED/GREEN do gate legal do site |
| `docs/stories/STORY-8.4-pci-concursos-legal-onboarding.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |
| `ADRs-web-scraping-vagas/ADR002-Target-Site-Taxonomy-and-Requirements-for-WebScraper.md` | Modificado | PCI atualizado com status formal de onboarding |

---

## Problemas encontrados

### Problema 1 — o projeto já suportava `legalStatus`, mas ainda sem checklist executável

O domínio já tinha `legalStatus` e a regra “APPROVED para produção”, mas isso ainda não estava
amarrado a uma validação explícita do checklist do ADR002.

### Problema 2 — o `robots.txt` do PCI está permissivo para listagens, mas isso não basta sozinho

Na verificação de 2026-03-13, o `robots.txt` retornou `200` e trouxe `Allow: /` para `User-agent: *`,
com `Disallow` pontuais para áreas administrativas, PDFs e PHP.

### Problema 3 — não foi encontrada página pública dedicada de ToS no fluxo exposto pelo site

Na homepage do PCI, o footer expõe apenas `Política de Privacidade e Cancelamento`. Sem termos
públicos claros autorizando o uso automatizado, o onboarding ainda fica incompleto.

### Problema 4 — a primeira versão do validator generalizava regras de scraping para fontes API

No review, apareceu uma regressão importante: `API_OFICIAL` e `DADOS_PUBLICOS` ainda podiam ser
marcados como `SCRAPING_PROIBIDO` por regras que só fazem sentido para scrapers HTML.

### Problema 5 — proibição explícita ainda podia ser mascarada como `PENDING_REVIEW`

No review seguinte, apareceu outro erro de precedência:

- quando o checklist já mostrava vedação explícita, campos em branco ainda podiam derrubar o
  resultado para `PENDING_REVIEW`
- isso escondia uma proibição real atrás de um estado genérico de documentação incompleta

### Problema 6 — checklist mal classificado podia burlar o gate técnico-legal

No review seguinte, apareceram duas lacunas:

- um scraper HTML ainda podia ser aprovado com checklist `API_OFICIAL` sem coerência com a metadata
  persistida do site
- o caminho de aprovação ainda devolvia `productionReady = true` preservando `enabled = false`,
  o que quebrava a primeira ativação

### Problema 7 — `DADOS_PUBLICOS` em HTML ainda podia cair em proibição de scraping

No review seguinte, apareceu a última lacuna:

- sites juridicamente classificados como `DADOS_PUBLICOS` continuavam entrando nas proibições
  específicas de scraping quando a extração técnica era `STATIC_HTML`
- isso contrariava a decisão documental da própria story e do ADR

### Problema 8 — APIs e dados públicos ainda sofriam dois bloqueios indevidos

No review seguinte, apareceram duas lacunas finais:

- `API_OFICIAL` e `DADOS_PUBLICOS` ainda exigiam uma URL dedicada de ToS, o que não faz sentido
  para fontes amparadas por base legal pública ou documentação oficial da API
- a coerência entre categoria legal e metadata técnica ainda estava validada só numa direção

### Problema 9 — checklist de ToS e denials explícitos ainda precisavam de nuance final

No review seguinte, apareceram dois ajustes finais:

- revisão de ToS continua obrigatória no checklist para qualquer site, mesmo que não exista uma URL
  dedicada de termos
- `DADOS_PUBLICOS` em HTML não pode ignorar denials explícitos de `robots.txt` ou ToS revisado

### Problema 10 — metadata API parcialmente inconsistente e termos negando automação

No review final, apareceram mais dois casos:

- um site com apenas metade da metadata de API configurada ainda podia ser tratado como API válido
- uma fonte API-backed ainda podia ser aprovada mesmo quando os termos revisados negavam acesso
  automatizado

### Problema 11 — drift de metadata API ainda podia virar proibição errada

No review final, apareceu a última precedência incorreta:

- quando o checklist dizia `API_OFICIAL`, mas a metadata persistida estava só parcialmente em modo
  API, um `robots.txt` de HTML ainda podia empurrar o resultado para `SCRAPING_PROIBIDO`
- nesse caso o erro real é drift de configuração, então o status correto continua sendo
  `PENDING_REVIEW`

### Problema 12 — mismatch `API_OFICIAL` x scraper HTML coerente ainda caía no caminho de proibição

No review final, apareceu o último caso residual:

- mesmo com metadata HTML coerente, um checklist `API_OFICIAL` ainda podia cair em
  `SCRAPING_PROIBIDO` por `robots.txt` restritivo
- isso ainda misturava erro de classificação de onboarding com proibição legal real

---

## Causa raiz

- até a 8.3 o foco estava em viabilidade técnica do scraper, não em ativação em produção
- `robots.txt` permissivo não resolve sozinho a incerteza de ToS
- o projeto precisava separar “scraper funciona” de “fonte está liberada para produção”
- a primeira versão do validator ainda não separava claramente o caminho de scraping do caminho
  API/public-data
- a precedência entre “proibido” e “incompleto” ainda não estava modelada corretamente
- a aprovação ainda não validava coerência entre o checklist legal e a estratégia técnica persistida
- o resultado aprovado ainda não estava materializando um site efetivamente ativável
- a exceção documental de `DADOS_PUBLICOS` ainda não estava refletida corretamente na lógica
- o validator ainda exigia ToS dedicado para categorias cujo amparo legal pode vir de outro tipo de
  documentação oficial
- o mismatch entre categoria legal e metadata técnica ainda não estava simétrico
- a regra ainda confundia “ToS revisado” com “ToS publicado em URL dedicada”
- a exceção de `DADOS_PUBLICOS` ainda estava ampla demais quando havia vedação explícita revisada
- a coerência de metadata API ainda aceitava estados parcialmente quebrados
- a negação explícita em ToS revisado ainda não estava bloqueando fontes API-backed
- a precedência entre drift de metadata API e proibição de scraping HTML ainda estava invertida
- o mismatch entre checklist `API_OFICIAL` e scraper HTML coerente ainda não estava saindo cedo
  o suficiente para permanecer em `PENDING_REVIEW`

---

## Solução aplicada

- criado um checklist formal de onboarding em código
- criada uma decisão derivada de checklist para materializar:
  - `APPROVED`
  - `PENDING_REVIEW`
  - `SCRAPING_PROIBIDO`
- garantido que checklist incompleto derruba `enabled` para `false`
- ajustado pós-review: a proibição por `robots`/ToS de scraping só é aplicada às categorias
  `SCRAPING_PERMITIDO` e `SCRAPING_PROIBIDO`
- ajustado pós-review final: proibição explícita revisada agora prevalece sobre pendências
  documentais, enquanto falta de evidência continua resultando em `PENDING_REVIEW`
- ajustado pós-review final:
  - `API_OFICIAL` agora exige compatibilidade com `ExtractionMode.API` / `SiteType.TYPE_E`
  - aprovação consistente agora materializa `enabled = true`
- ajustado pós-review final:
  - proibições específicas de scraping agora só atingem categorias `SCRAPING_*`
  - `DADOS_PUBLICOS` permanece fora desse bloqueio, mesmo com implementação HTML
- ajustado pós-review final:
  - `API_OFICIAL` e `DADOS_PUBLICOS` não exigem mais `termsOfServiceUrl` dedicado
  - o validator rejeita tanto `API_*` em scraper HTML quanto `SCRAPING_*` em site API-backed
- ajustado pós-review final:
  - `termsReviewed` continua obrigatório para aprovação em qualquer categoria
  - `termsOfServiceUrl` dedicado só é obrigatório para categorias `SCRAPING_*`
  - `DADOS_PUBLICOS` em HTML só fica isento de bans genéricos; vedação explícita revisada continua
    resultando em bloqueio
- ajustado pós-review final:
  - metadata API só é aceita quando `ExtractionMode.API` e `SiteType.TYPE_E` estão alinhados juntos
  - `termsAllowScraping = false` com revisão concluída agora bloqueia também fontes API-backed
- ajustado pós-review final:
  - inconsistência de metadata API com checklist `API_OFICIAL` agora retorna `PENDING_REVIEW` antes
    de qualquer proibição derivada de `robots` HTML
- ajustado pós-review final:
  - qualquer mismatch de `API_OFICIAL` com metadata que não seja realmente API agora retorna
    `PENDING_REVIEW` antes do caminho de proibição
- formalizada a decisão atual do PCI:
  - `robots.txt`: revisado e permissivo para listagens públicas
  - API oficial: não identificada
  - ToS público dedicado: não identificado no fluxo público revisado
  - resultado atual: `legalStatus = PENDING_REVIEW`
  - produção: **bloqueada**

Checklist ADR002 preenchido para o PCI nesta iteração:

```text
✓ URL do robots.txt revisada e resultado documentado
⚠ ToS revisado parcialmente; página pública dedicada não identificada
✓ API oficial verificada e não encontrada
✓ Justificativa de negócio registrada
✓ Perfil de rate-limit definido
✓ Categoria legal registrada
✓ Contato/equipe responsável registrado
✓ Status de autenticação documentado
✓ Evidência de HTML público / descoberta registrada
```

Fontes primárias usadas na revisão:

- `https://www.pciconcursos.com.br/robots.txt`
- `https://www.pciconcursos.com.br/`
- `https://www.pciconcursos.com.br/politica-de-privacidade/`

---

## Lições aprendidas

- ativação em produção precisa de gate explícito, não só de convenção documental
- PCI já está tecnicamente pronto para scraping, mas ainda não está juridicamente pronto para produção
- a modelagem de onboarding criada aqui prepara diretamente a expansão segura da Iteration 9

---

## Estado final

Resultado validado:

```text
Tests run: 214, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comandos executados:

```bash
./mvnw test -DexcludedGroups=integration -Dtest=TargetSiteOnboardingValidatorTest
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- onboarding validator implementado
- PCI permanece `PENDING_REVIEW` para produção
- story pronta para review
