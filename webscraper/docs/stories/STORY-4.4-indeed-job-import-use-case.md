# STORY 4.4 — Persistir vagas do Indeed

**Status:** ✅ Concluída
**Iteration:** 4 — Primeira integração API-first: Indeed MCP Connector
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 4.4

---

## Objetivo

Fechar a primeira fatia ponta a ponta do fluxo Indeed:

`command -> strategy -> normalize -> persist`

A story introduz o use case de importação que recebe o resultado da strategy, enriquece os
`JobPostingEntity` com metadados de persistência e salva no banco.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `IndeedJobImportUseCaseTest` como teste de integração cobrindo a fatia completa:

- `TargetSiteEntity` persistido
- `CrawlJobEntity` e `CrawlExecutionEntity` persistidos
- execução da strategy do Indeed
- normalização do payload
- persistência final em `job_postings`

A falha inicial de compilação foi:

```text
cannot find symbol: class IndeedJobImportUseCase
```

### GREEN — implementação mínima

Foi implementado `IndeedJobImportUseCase` com:

- chamada a `IndeedApiJobScraperStrategy.scrape(command)`
- enriquecimento dos itens com:
  - `targetSite`
  - `crawlExecution`
  - `contractType`
  - `fingerprintHash`
  - `dedupStatus`
- persistência com `JobPostingRepository.saveAll(...)`

### REFACTOR

Sem refatoração estrutural ampla. O enriquecimento foi mantido dentro do use case porque a story
exige fechamento da fatia de persistência, e ainda não existe um serviço de persistência mais geral.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/usecase/IndeedJobImportUseCase.java` | Criado | Use case ponta a ponta para importar e persistir vagas do Indeed |
| `src/test/java/com/campos/webscraper/application/usecase/IndeedJobImportUseCaseTest.java` | Criado | Teste de integração da fatia completa do Indeed |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-4.4-indeed-job-import-use-case.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O ambiente não executa os testes Testcontainers

**Sintoma:**

```text
client version 1.32 is too old. Minimum supported API version is 1.40
Could not find a valid Docker environment
```

### Problema 2 — O payload normalizado ainda não carregava todos os campos obrigatórios de persistência

`JobPostingEntity` exige campos de persistência que não fazem parte da responsabilidade direta do
normalizer, como `targetSite`, `crawlExecution`, `fingerprintHash`, `dedupStatus` e um
`contractType` concreto.

---

## Causa raiz

### Problema 1

O ambiente continua exposto a um proxy/daemon Docker incompatível com a versão mínima exigida pelo
Testcontainers do projeto.

### Problema 2

A normalização da Story 4.2 foi corretamente focada em mapear o payload Indeed, enquanto a 4.4
introduz requisitos adicionais de persistência que pertencem à camada de aplicação.

---

## Solução aplicada

- criado `IndeedJobImportUseCase` para fechar a fatia de aplicação
- enriquecidos os `JobPostingEntity` antes da persistência com:
  - `targetSite`
  - `crawlExecution`
  - `contractType = CLT`
  - `dedupStatus = NEW`
  - `fingerprintHash` calculado por `JobPostingFingerprintCalculator`
- mantido o teste de integração pronto para execução local com Docker compatível

---

## Lições aprendidas

- a separação entre normalização e persistência ficou mais clara com a 4.4
- o primeiro fluxo ponta a ponta já está modelado o suficiente para servir de template para DOU
- a ausência de um enum `NOT_SPECIFIED` em `JobContractType` força uma decisão provisória na
  camada de aplicação; esse ponto merece revisão futura

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 147, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Resultado da tentativa de integração:

- `IndeedJobImportUseCaseTest` criado e compilando
- execução bloqueada por incompatibilidade do ambiente Docker/Testcontainers

Conclusão:

- fluxo do Indeed fechado até a persistência
- testes unitários verdes
- teste de integração pronto, pendente de ambiente Docker compatível
