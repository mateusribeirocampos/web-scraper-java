# STORY 9.3 — Implementar GreenhouseJobNormalizer

**Status:** ✅ Concluída
**Iteration:** 9 — Expansão do setor privado para PMEs via ATS público
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 9.3

---

## Objetivo

Mapear o payload da Greenhouse Job Board API para `JobPostingEntity`, preparando a integração PME
para a strategy da 9.4:

- normalizar `title`, `company`, `location`, `canonicalUrl`, `publishedAt` e `description`
- preservar o payload bruto como JSON para auditoria
- derivar sinais básicos de `seniority`, `remote` e `techStackTags`

---

## Ciclo TDD

### RED — mapeamento de campos da Greenhouse primeiro

Foi criado `GreenhouseJobNormalizerTest` cobrindo:

- mapeamento dos campos centrais para `JobPostingEntity`
- inferência de `seniority`, `remote` e `techStackTags`
- preservação do payload original em `payloadJson`

O RED inicial falhou por compilação, porque ainda não existia `GreenhouseJobNormalizer`.

### GREEN — implementação mínima

Foi implementado:

1. `GreenhouseJobNormalizer`

Contrato entregue:

- `externalId` = `id` da Greenhouse convertido para `String`
- `canonicalUrl` = `absolute_url`
- `company` = `company_name`
- `location` = `location.name`
- `publishedAt` = `first_published` convertido para `LocalDate`
- `description` = `content`
- `payloadJson` preserva o shape original da API
- heurística leve:
  - `Senior`/`Sr` => `SENIOR`
  - `Junior`/`Jr` => `JUNIOR`
  - `Intern` => `INTERN`
  - `Lead`/`Staff`/`Principal` => `LEAD`
  - `Mid`/`Pleno` => `MID`
  - presença de `java` em título/conteúdo => `techStackTags = "Java"`
  - `Latin America`, `LATAM`, `Remote` ou `Remoto` => `remote = true`

### REFACTOR

O normalizer foi mantido sem dependência de `TargetSite`, persistência ou factory. A story precisava
fechar apenas a transformação do payload Greenhouse para o modelo canônico de vaga privada.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/normalizer/GreenhouseJobNormalizer.java` | Criado | Mapeia payload Greenhouse para `JobPostingEntity` |
| `src/test/java/com/campos/webscraper/application/normalizer/GreenhouseJobNormalizerTest.java` | Criado | RED/GREEN do normalizer |
| `docs/stories/STORY-9.3-greenhouse-job-normalizer.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — o payload auditável preserva o shape externo, não o nome dos campos internos

No RED apareceu um detalhe importante:

- `payloadJson` preserva o contrato externo da Greenhouse
- isso significa JSON com `company_name`, `absolute_url` e `first_published`
- o teste inicial esperava o nome Java do record (`companyName`) e falhou

### Problema 2 — o payload da Greenhouse não entrega `seniority` e `remote` normalizados

Esses sinais precisaram ser derivados de forma conservadora a partir de `title` e `location`.

### Problema 3 — heurísticas iniciais ainda erravam `remote` e `seniority` em casos realistas

No review apareceram duas lacunas importantes:

- algumas vagas Greenhouse marcam remoto em `location.country = "Remote"`, não só em
  `location.name`
- o matching ingênuo por substring podia classificar errado títulos como `SRE Engineer`,
  `Middleware Engineer` e `Internal Tools Engineer`

### Problema 4 — o detector de stack Java confundia `Java` com `JavaScript`

No review apareceu mais uma lacuna de qualidade:

- a heurística inicial de `techStackTags` procurava `java` por substring crua
- isso classificava vagas de frontend/full-stack com `JavaScript` como se fossem vagas `Java`

---

## Causa raiz

- a Greenhouse Job Board API é um payload de transporte, não um modelo de domínio
- o projeto ainda não tinha heurística local para extrair senioridade/remoto dessa fonte
- o audit trail precisa manter o contrato externo, não a forma interna do Java
- a primeira heurística de normalização ainda estava permissiva demais para títulos livres
- a mesma permissividade também contaminava a detecção de stack quando um termo era prefixo de outro

---

## Solução aplicada

- criado `GreenhouseJobNormalizer`
- feito parse de `first_published` com `OffsetDateTime`
- preservado o payload bruto com serialização do DTO original
- adicionadas heurísticas leves e explícitas para `seniority`, `remote` e `techStackTags`
- corrigido o teste para validar o JSON auditável com nomes reais da API
- ajustado pós-review: `remote` agora também considera `location.country`
- ajustado pós-review: senioridade por abreviação agora usa match por palavra/token, evitando falsos
  positivos por substring
- ajustado pós-review: detecção de stack `Java` agora também usa match por palavra/token, evitando
  falsos positivos em `JavaScript`

---

## Lições aprendidas

- o JSON de auditoria deve refletir a fonte externa original, não o naming interno do projeto
- vale manter heurísticas de normalização pequenas e defensáveis nesta fase
- a Greenhouse já entrega dados suficientes para a 9.4 sem reabrir a borda HTTP
- heurística de títulos livres precisa ser conservadora; substring crua vira ruído rápido
- a mesma disciplina vale para stack tags; prefixo textual não é evidência suficiente de tecnologia

---

## Estado final

- `GreenhouseJobNormalizer` implementado
- mapeamento principal da Greenhouse para `JobPostingEntity` verde
- heurísticas mínimas de `seniority`, `remote` e `techStackTags` implementadas
- testes unitários da fatia verdes

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=GreenhouseJobNormalizerTest`

Próximo passo natural:

- Story 9.4 — implementar `GreenhouseJobScraperStrategy`
