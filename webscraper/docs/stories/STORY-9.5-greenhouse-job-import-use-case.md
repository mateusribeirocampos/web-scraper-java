# STORY 9.5 — Persistir vagas PME via Greenhouse

**Status:** ✅ Concluída
**Iteration:** 9 — Expansão do setor privado para PMEs via ATS público
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 9.5

---

## Objetivo

Fechar a fatia ponta a ponta da integração Greenhouse:

- executar `command -> strategy -> normalize -> persist`
- enriquecer os registros com `targetSite`, `crawlExecution`, `dedupStatus` e `fingerprintHash`
- deixar o board `greenhouse_bitso` pronto para consulta persistida na API da aplicação

---

## Ciclo TDD

### RED — teste de integração do slice completo primeiro

Foi criado `GreenhouseJobImportUseCaseTest` cobrindo:

- persistência ponta a ponta de vagas Greenhouse em `job_postings`
- vínculo com `TargetSiteEntity` e `CrawlExecutionEntity`
- geração de `fingerprintHash`

O RED inicial falhou por compilação, porque ainda não existia `GreenhouseJobImportUseCase`.

### GREEN — implementação mínima

Foi implementado:

1. `GreenhouseJobImportUseCase`

Contrato entregue:

- executa `GreenhouseJobScraperStrategy`
- enriquece `JobPostingEntity` com metadados de persistência
- usa `contractType` inferido quando existir e faz fallback para `UNKNOWN`
- define `dedupStatus = NEW`
- calcula `fingerprintHash`
- persiste o lote em `JobPostingRepository`

### REFACTOR

O use case manteve o mesmo padrão já consolidado em Indeed:

- strategy focada em extração
- use case focado em enriquecimento e persistência

Isso preserva uniformidade entre providers privados API-first.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/usecase/GreenhouseJobImportUseCase.java` | Criado | Persistência ponta a ponta das vagas Greenhouse |
| `src/test/java/com/campos/webscraper/application/usecase/GreenhouseJobImportUseCaseTest.java` | Criado | Teste de integração do slice completo |
| `docs/stories/STORY-9.5-greenhouse-job-import-use-case.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — a 9.4 ainda devolvia apenas itens em memória

A strategy Greenhouse já funcionava, mas ainda faltava a etapa que transforma o resultado extraído
em registros persistidos e auditáveis no banco.

### Problema 2 — o provider privado Greenhouse precisava fechar o mesmo contrato operacional do Indeed

Sem essa story, o projeto teria dois níveis de maturidade diferentes para fontes privadas API-first:

- Indeed completo até persistência
- Greenhouse apenas até strategy

---

## Causa raiz

- a iteração 9 foi dividida corretamente em onboarding, client, normalizer, strategy e persistência
- o enriquecimento para gravação em `job_postings` precisa de contexto que não pertence ao client
  nem à strategy
- a fonte Greenhouse atual não entrega regime contratual canônico com confiança suficiente para
  assumir um vínculo brasileiro específico

---

## Solução aplicada

- criado `GreenhouseJobImportUseCase`
- reutilizado o padrão de enriquecimento já estabelecido no fluxo do Indeed
- validada a gravação de duas vagas do board `bitso` com contexto de crawl
- ajustado pós-review: o import Greenhouse deixou de forçar `CLT` e passou a usar fallback neutro
  `UNKNOWN` quando a fonte não informa regime contratual com confiança
- documentado também o cenário de aceite manual esperado para esta família de fonte:
  - disparar um crawl job Greenhouse alinhado à intenção `desenvolvedor de software em java spring boot`
  - confirmar persistência
  - consultar `GET /api/v1/job-postings`

---

## Lições aprendidas

- a separação entre strategy e use case continua adequada quando o provider é API-first
- o padrão de persistência privada agora ficou homogêneo entre Indeed e Greenhouse
- a próxima expansão para Lever tende a reaproveitar quase integralmente este formato

---

## Estado final

- `GreenhouseJobImportUseCase` implementado
- slice completo Greenhouse -> persistência fechado
- board `greenhouse_bitso` pronto para a camada de leitura após execução do import
- testes do slice verdes

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=GreenhouseJobImportUseCaseTest`

Próximo passo natural:

- Story 9.6 — generalizar provider ATS para Lever
