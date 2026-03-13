# STORY 10.2 — Worker de execução

**Status:** ✅ Concluída
**Iteration:** 10 — Processamento assíncrono
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 10.2

---

## Objetivo

Introduzir o primeiro worker real do pipeline assíncrono:

- consumir mensagens da fila
- reconstruir ou recarregar o `CrawlJob`
- despachar a execução
- resolver o import correto por source já implementada

---

## Ciclo TDD

### RED — worker e runner end-to-end primeiro

Os primeiros testes da fatia cobriram:

- worker consumindo mensagem persistida e chamando o dispatcher
- worker consumindo mensagem transitória e reconstruindo o `CrawlJob`
- runner resolvendo `Indeed`, `Greenhouse` e `DOU`
- dispatcher persistindo lifecycle com o novo contrato do runner

O RED inicial quebrou por compilação, porque o contrato antigo do runner ainda não carregava o
`CrawlExecutionEntity`, e a mensagem de fila ainda não trazia URL suficiente para reconstrução
transitória.

### GREEN — implementação mínima

Foi implementado:

1. `ImportingCrawlJobExecutionRunner`
2. `CrawlJobWorker`
3. evolução do envelope `EnqueuedCrawlJob` com `targetUrl`
4. ajuste do dispatcher para passar `CrawlExecutionEntity` ao runner
5. ajuste do scheduler para atuar como producer da fila

Contrato entregue:

- o worker consome a mensagem da fila
- se houver `crawlJobId`, ele recarrega o job persistido
- se não houver `id`, ele reconstrói e persiste um job transitório a partir do snapshot materializado
- o dispatcher continua responsável pelo lifecycle da execução
- o runner resolve o import por `siteCode` e persiste o output via use cases já existentes
- o scheduler passa a enfileirar jobs elegíveis, e o worker passa a drená-los em produção

### REFACTOR

O desenho manteve duas bordas pequenas:

- `CrawlJobWorker` cuida de fila + carregamento/reconstrução
- `ImportingCrawlJobExecutionRunner` cuida de import + persistência

Isso evita acoplar fila, lifecycle e regras por source no mesmo objeto.

### Ajuste pós-review

Após o review, o worker foi endurecido para refletir o modelo persistente real:

- mensagens transitórias agora só seguem se trouxerem `targetSiteId` persistido
- o worker salva o `CrawlJobEntity` reconstruído antes de despachar
- mensagens sem `targetSiteId` deixam de ser tratadas como caminho executável
- falhas de lookup após o consumo agora reencaminham a mensagem para `DEAD_LETTER_JOBS`
- a reconstrução transitória preserva a `targetUrl` do snapshot enfileirado, mesmo se o `TargetSite` tiver sido alterado depois
- falhas transitórias de repositório/dispatcher agora retornam a mensagem para a fila original
- `ImportingCrawlJobExecutionRunner`, `InMemoryCrawlJobQueue`, `CrawlJobQueueRouter` e `CrawlJobWorker` passaram a ficar registrados no contexto Spring
- os use cases de import (`Indeed`, `Greenhouse`, `DOU`) passaram a ser beans reais, fechando o wiring do runner na aplicação
- o dispatcher passou a devolver o `CrawlExecutionStatus` final, permitindo ao worker reenfileirar execuções que terminaram em `FAILED`
- o caminho transitório salva o `CrawlJob` com `TargetSite` gerenciado e só depois despacha uma cópia com snapshot de crawl, evitando associação JPA destacada
- retries de mensagens transitórias agora preservam o `crawlJobId` persistido, impedindo duplicação de linhas em `crawl_jobs`
- execuções finalizadas como `DEAD_LETTER` agora também movem o envelope para `DEAD_LETTER_JOBS`
- o runner passou a suportar `pci_concursos`, cobrindo a fila `STATIC_SCRAPE_JOBS` já existente
- retries de jobs transitórios agora continuam usando os overrides do envelope (`targetUrl`, `siteCode`, `extractionMode`) mesmo depois de já existir `crawlJobId`
- jobs transitórios sem `jobCategory` explícita agora herdam a categoria efetiva do `TargetSite`, inclusive para fontes de concurso
- retries do worker agora têm cap e backoff antes de voltar para a fila original; ao esgotar tentativas, a mensagem segue para `DEAD_LETTER_JOBS`
- jobs transitórios persistidos pelo worker agora ficam marcados como não gerenciados pelo scheduler, evitando reenqueue recorrente de one-offs
- a fila em memória agora ignora itens atrasados no topo e continua consumindo mensagens prontas atrás deles, evitando starvation por backoff
- o scheduler foi mantido como producer simples de fila, sem tentar desarmar o `CrawlJob` recorrente no banco durante o handoff para a fila em memória

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/orchestrator/CrawlJobWorker.java` | Criado | Consumer da fila que entrega jobs ao dispatcher |
| `src/main/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunner.java` | Criado | Runner real que resolve imports por source |
| `src/main/java/com/campos/webscraper/application/orchestrator/CrawlJobExecutionRunner.java` | Modificado | Contrato agora recebe `CrawlExecutionEntity` |
| `src/main/java/com/campos/webscraper/application/orchestrator/CircuitBreakingCrawlJobDispatcher.java` | Modificado | Passa o `CrawlExecutionEntity` corrente ao runner |
| `src/main/java/com/campos/webscraper/application/orchestrator/NoOpCrawlJobExecutionRunner.java` | Modificado | Adequação ao novo contrato |
| `src/main/java/com/campos/webscraper/application/queue/EnqueuedCrawlJob.java` | Modificado | Snapshot agora inclui `targetUrl` |
| `src/main/java/com/campos/webscraper/application/usecase/IndeedJobImportUseCase.java` | Modificado | Registro Spring do import use case |
| `src/main/java/com/campos/webscraper/application/usecase/GreenhouseJobImportUseCase.java` | Modificado | Registro Spring do import use case |
| `src/main/java/com/campos/webscraper/application/usecase/DouContestImportUseCase.java` | Modificado | Registro Spring do import use case |
| `src/main/java/com/campos/webscraper/application/usecase/PciConcursosImportUseCase.java` | Criado | Import ponta a ponta para `pci_concursos` |
| `src/main/resources/db/migration/V006__add_scheduler_managed_to_crawl_jobs.sql` | Criado | Flag para separar jobs recorrentes de jobs one-off do worker |
| `src/test/java/com/campos/webscraper/application/orchestrator/CrawlJobWorkerTest.java` | Criado | RED/GREEN do worker |
| `src/test/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunnerTest.java` | Criado | RED/GREEN do runner real |
| `src/test/java/com/campos/webscraper/application/orchestrator/CircuitBreakingCrawlJobDispatcherTest.java` | Modificado | Ajuste ao novo contrato do runner |
| `src/test/java/com/campos/webscraper/application/usecase/ExecuteCrawlJobManuallyUseCaseTest.java` | Modificado | Ajuste ao contrato novo do dispatcher |
| `src/test/java/com/campos/webscraper/application/queue/CrawlJobQueueTest.java` | Modificado | Envelope atualizado com `targetUrl` |
| `docs/stories/STORY-10.2-crawl-job-worker.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — o worker precisava entregar persistência real, não só counters

O dispatcher já controlava `RUNNING/SUCCEEDED/FAILED`, mas o runner antigo não recebia o
`CrawlExecutionEntity`, então não conseguia persistir postings ligados à execução corrente.

### Problema 2 — a fila já aceitava jobs transitórios, mas o consumer ainda não

Depois da 10.1, a abstração já suportava snapshots sem `id`, porém o consumer ainda precisava
reconstruir um `CrawlJobEntity` executável a partir do envelope.

---

## Causa raiz

- a lifecycle de execução e os imports ponta a ponta foram implementados em iterações diferentes
- a 10.1 estabilizou a fila, mas ainda faltava conectar consumo + import + persistência

---

## Solução aplicada

- contrato do runner evoluído para receber `CrawlExecutionEntity`
- criado `ImportingCrawlJobExecutionRunner` usando os imports já existentes
- criado `CrawlJobWorker` como consumer explícito da fila
- enriquecido o envelope da fila com `targetUrl` para reconstrução de jobs transitórios
- exigido `targetSiteId` persistido para execução real do caminho transitório
- registrado o runner e os imports no contexto Spring
- evoluído o dispatcher para devolver o status final da execução
- separado `TargetSite` gerenciado para persistência de `CrawlJob` e snapshot materializado para execução do crawl
- preservado o `crawlJobId` gerado ao reenfileirar jobs transitórios
- enviado `DEAD_LETTER` também para a dead-letter queue da fila
- adicionado o import use case de `pci_concursos` ao runner do worker
- reaplicado o snapshot do envelope também no caminho de retry com `crawlJobId`
- corrigida a herança da `jobCategory` no caminho transitório para evitar default incorreto em fontes públicas
- introduzido cap/backoff de retry no envelope da fila para evitar loop quente infinito
- introduzido `schedulerManaged` em `CrawlJobEntity` para impedir que o scheduler reenqueue jobs transitórios criados pelo worker
- ajustado o consumidor da fila para procurar o primeiro item elegível em vez de bloquear a fila inteira no head atrasado
- mantido o scheduler como producer não destrutivo enquanto a fila for apenas `InMemoryCrawlJobQueue`

### Limitação operacional explicitada

Enquanto o projeto usar apenas `InMemoryCrawlJobQueue`, o handoff `DB -> queue` não é durável nem
atômico. Por isso, a Story 10.2 deliberadamente **não** tenta desarmar o `CrawlJob` recorrente no
banco ao enfileirar.

Trade-off aceito nesta story:

- pode haver reenqueue do mesmo job recorrente se ele continuar elegível no scheduler
- a fila em memória continua servindo como seam de orquestração, não como broker confiável
- a proteção correta contra duplicatas repetidas fica como objetivo explícito da Story 10.3
- um handoff durável/atômico exige fila persistida ou padrão outbox, fora do escopo desta fatia

---

## Lições aprendidas

- lifecycle e persistência de items precisam compartilhar o mesmo `CrawlExecutionEntity`
- snapshot de fila precisa carregar informação suficiente para reconstrução mínima
- separar worker e runner reduz bastante o acoplamento do pipeline assíncrono

---

## Estado final

- worker de execução implementado
- runner real de import implementado para fontes já suportadas
- consumo de mensagens persistidas e transitórias validado
- dispatcher adaptado ao novo contrato
- limitação do handoff com fila em memória documentada explicitamente

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=CrawlJobSchedulerTest,CrawlJobWorkerTest,CrawlJobQueueTest,ImportingCrawlJobExecutionRunnerTest,CircuitBreakingCrawlJobDispatcherTest,ExecuteCrawlJobManuallyUseCaseTest`
- `./mvnw test -DexcludedGroups=integration`

Próximo passo natural:

- Story 10.3 — Idempotência e prevenção de duplicatas
