# ADR 009 — Plano de Entrega XP e Tarefas Detalhadas para o WebScraper de Vagas

## Title

Plano de implementação orientado a XP (Extreme Programming) com tarefas granulares para a
plataforma WebScraper de vagas Java Júnior / Spring Boot e concursos públicos brasileiros.

## Status

Accepted

## Date

2026-03-09

## Context

O projeto precisa de um roadmap de implementação prático que traduza a arquitetura em
deliverables granulares. O plano torna o TDD explícito em cada feature, identifica as primeiras
integrações API-first a serem implementadas (Indeed MCP e DOU API) e progride de forma
incremental até os scrapers HTML estáticos e o fallback dinâmico.

Após a conclusão das primeiras fatias Indeed e DOU, o roadmap passa a explicitar também a
expansão para vagas de empresas pequenas e médias por meio de providers ATS públicos
(Greenhouse/Lever/Ashby) e páginas de carreira próprias com onboarding legal aprovado. O projeto
não deve ficar limitado a concursos nem depender cedo de boards generalistas com risco jurídico
mais alto.

---

## XP Delivery Rules

- Stories pequenas com valor de negócio visível.
- Red → Green → Refactor mandatório em toda story.
- Pair-review ou code review estruturado para regras de parser.
- Integração contínua obrigatória antes de merge.
- Refatoração faz parte do critério de pronto, não é cleanup opcional.
- Toda nova família de fonte deve fechar também um teste manual de aceite orientado ao usuário.

### Fluxo Oficial de Execução das Tasks

Toda task deve seguir a mesma ordem operacional:

1. verificar se a task continua coerente com o estado atual do projeto, ADRs, stories, commits
   recentes e `README.md`;
2. abrir a implementação por teste, seguindo XP/TDD;
3. aplicar Red → Green → Refactor até os testes focados ficarem aprovados;
4. executar validação real com a aplicação rodando para observar erros de runtime e comportamento
   operacional;
5. só depois enviar para review;
6. só depois de review aprovada fazer `commit/push` para `main`;
7. antes do `push`, sincronizar documentação e histórico da entrega.

### Regra Atual de Testes

- o projeto continua TDD-first;
- mas o fluxo oficial da equipe não depende de Testcontainers como gate obrigatório;
- a validação principal deve combinar:
  - testes automatizados focados;
  - fixtures e mocks controlados quando fizer sentido;
  - testes reais com o aplicativo em execução;
- menções antigas a Testcontainers neste ADR representam contexto histórico de desenho, não uma
  exigência operacional atual.

### Manual User Acceptance Rule

Além dos testes automatizados, cada nova integração de fonte deve prever um cenário manual
reproduzível de verificação funcional no sistema rodando.

Formato mínimo do cenário:

1. Disparar o job manualmente.
2. Confirmar que a execução concluiu com sucesso.
3. Consultar os endpoints de leitura do sistema.
4. Verificar se os itens retornados correspondem à intenção de busca do usuário.

Consultas de referência:

- vagas privadas: `desenvolvedor de software em java spring boot`
- concursos: `concurso analista de ti` ou `concurso desenvolvedor java`

Observação importante:

- enquanto a plataforma ainda não expuser busca customizada como input público, esse teste manual
  pode ser executado com um `CrawlJob` pré-configurado ou uma URL/endpoint de origem que já
  represente a pesquisa desejada;
- quando a busca customizada virar feature do produto, esse mesmo cenário passa a ser requisito
  explícito de aceite end-to-end.

### Current Project Usage For User Validation

No estado atual do projeto em 2026-03-13, o uso manual para validar scraping funciona assim:

1. Configurar um `CrawlJob` cuja fonte represente a busca desejada.
2. Disparar:
   - `POST /api/v1/crawl-jobs/{jobId}/execute`
3. Acompanhar a execução persistida.
4. Consultar a saída persistida:
   - `GET /api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND&seniority=...`
   - `GET /api/v1/public-contests?...`
5. Confirmar se os resultados correspondem à intenção do usuário.

No perfil oficial `JAVA_JUNIOR_BACKEND`, a listagem deve priorizar resultado útil e ainda aplicável:
- recência obrigatória;
- exclusão de banco de talentos;
- exclusão de cargos de gestão/liderança;
- exclusão de senioridade `SENIOR/LEAD`;
- presença de stack aderente (`Java`, `Spring` ou `Kotlin`);
- presença de função aderente (`backend`, `developer`, `software engineer`, `desenvolvedor`, etc.).

Se o objetivo for apenas exploração ampla da base, o contrato admite `profile=UNFILTERED`.
Se o objetivo for um meio-termo entre aderência estrita e exploração ampla, o contrato também
admite `profile=JAVA_BACKEND_BALANCED`.
Se o objetivo for volume real com stack aderente, mesmo sem fit estrito de função, o contrato
também admite `profile=JAVA_STACK_PRAGMATIC`.

Exemplos de intenção já adotados como referência:

- `desenvolvedor de software em java spring boot`
- `concurso analista de ti`

Portanto, o projeto já suporta validação funcional real pelo usuário, mas ainda não expõe busca
livre customizada como parâmetro público de produto. Isso continua como evolução futura.

Atualização municipal em 2026-03-30:

- `Inconfidentes` e `Pouso Alegre` já operam como fontes municipais `PUBLIC_CONTEST` com HTML + PDF.
- A `13.2.6` consolidou a base comum de import municipal.
- A `13.2.5` abre `Munhoz` como o terceiro município reutilizando esse pipeline.

Atualização de priorização híbrida em 2026-03-31:

- após estabilizar `Inconfidentes`, `Pouso Alegre` e `Munhoz`, o próximo backlog deixa de ser
  apenas municipal isolado e passa a priorizar polos com chance real de gerar:
  - vagas privadas em empresas de tecnologia;
  - concursos e processos seletivos públicos relevantes para TI.
- backlog híbrido priorizado:
  1. `Campinas`
  2. `Santa Rita do Sapucaí`
  3. `Itajubá`
  4. `Poços de Caldas`
  5. `Extrema`
- racional:
  - `Campinas` tem o maior potencial combinado de mercado privado + setor público;
  - `Santa Rita do Sapucaí` e `Itajubá` são polos tecnológicos claros no Sul de Minas;
  - `Poços de Caldas` e `Extrema` permanecem fortes, mas ficam logo depois na fila.

Atualização de Santa Rita do Sapucaí em 2026-04-05:

- a trilha privada `lever_watchguard` já foi validada tecnicamente em runtime real, mas permanece
  `PENDING_REVIEW` por base legal ainda insuficiente para promoção automática;
- a frente ativa seguinte passa a ser a trilha pública oficial da Câmara Municipal;
- a página `Processos Seletivos 2025` entra como fonte `STATIC_HTML` com anexos PDF oficiais;
- a decisão da fatia foi modelar o HTML como fonte primária de metadados, usando os PDFs oficiais
  como `editalUrl` canônico, sem depender de enrichment PDF nesta primeira entrega.

---

## Detailed Tasks by Iteration

### Iteration 1 — Foundation e esqueleto do projeto

#### Story 1.1 — Bootstrap do projeto Spring Boot
- Criar projeto base Spring Boot com Maven (módulos ou estrutura de pacotes).
- Adicionar dependências: Web, Validation, Data JPA, Actuator, Test, jsoup,
  OkHttp, Resilience4j.
- Configurar `application.properties` base.
- Configurar Flyway para migrations.
- **TDD:** iniciar com context-load test e configuration validation test.

#### Story 1.2 — Criar enums e value objects do domínio de vagas
- Definir `SiteType`, `ExtractionMode`, `JobCategory`, `LegalStatus`.
- Definir `JobContractType`, `SeniorityLevel`, `GovernmentLevel`, `EducationLevel`.
- Definir `ContestStatus`, `DedupStatus`, `CrawlExecutionStatus`.
- Definir `ScrapeCommand`, `ScrapeResult<T>`, `FetchedPage`, `FetchRequest`.
- **TDD:** testes de invariantes de value objects antes de qualquer código de produção.

#### Story 1.3 — Criar arquitetura de pacotes
- `domain` (entidades, value objects, enums, interfaces de repositório)
- `application` (use cases, orchestrators, normalizers)
- `infrastructure` (JPA entities, repositories, fetchers, API clients)
- `interfaces` (REST controllers, schedulers)
- `shared` (utils, fingerprint, exceptions)
- **TDD:** smoke test de wiring de beans para o primeiro use case.

---

### Iteration 2 — Modelo de persistência

#### Story 2.1 — Implementar `TargetSiteEntity`
- Criar entidade e repository.
- Adicionar migration V001.
- Adicionar campos `jobCategory`, `legalStatus`, `selectorBundleVersion`.
- **TDD:** repository test/controlado primeiro, sem depender de Testcontainers como gate.

#### Story 2.2 — Implementar `CrawlJobEntity` e `CrawlExecutionEntity`
- Criar entidades, relacionamentos, índices.
- Adicionar migrations V002.
- **TDD:** testes de integração para persistência de relacionamentos e transições de status primeiro.

#### Story 2.3 — Implementar `JobPostingEntity` e campos de deduplicação
- Criar entidade com `publishedAt`, `fingerprintHash`, `contractType`, `seniority`, `techStackTags`.
- Adicionar migration V004.
- Implementar `JobPostingFingerprintCalculator`.
- **TDD:** entity mapping test + repository test + dedup rule test primeiro.

#### Story 2.4 — Implementar `PublicContestPostingEntity`
- Criar entidade com `contestName`, `organizer`, `registrationEndDate`, `numberOfVacancies`,
  `contestStatus`, etc.
- Adicionar migration V005.
- Implementar `ContestPostingFingerprintCalculator`.
- **TDD:** entity mapping test + repository test + test de consulta por `registrationEndDate` primeiro.

#### Story 2.5 — Baseline de persistência para `RawSnapshot`
- Materializar a entidade `RawSnapshotEntity` e seu repository para snapshots HTTP brutos.
- Persistir `siteCode`, `crawlExecutionId` opcional, `fetchedAt`, `responseStatus` e `responseBody`.
- Adicionar migration real no topo da sequência publicada, sem renumerar migrations já distribuídas.
- Escopo desta fatia:
  - baseline de persistência e testes de repositório;
  - sem prometer captura automática no runtime ainda.
- **TDD:** repository integration test primeiro.

---

### Iteration 3 — Contratos de Strategy e Factory

#### Story 3.1 — Criar contrato `JobScraperStrategy`
- Adicionar interface e tipos de resultado.
- **TDD:** definir contract tests primeiro.

#### Story 3.2 — Criar `JobScraperFactory`
- Resolver strategy por metadados explícitos do site.
- Lançar `UnsupportedSiteException` descritiva quando sem suporte.
- **TDD:** testes de resolução falhando primeiro (para cada strategy planejada).

#### Story 3.3 — Criar abstração `JobFetcher`
- Contrato de interface `JobFetcher` com implementação `HttpJobFetcher` (OkHttp).
- **TDD:** testes com transporte mockado (`WireMock` ou servidor HTTP equivalente) primeiro.

---

### Iteration 4 — Primeira integração API-first: Indeed MCP Connector

Esta é a integração de maior valor e menor risco técnico. Deve ser a **primeira source implementada**.

#### Story 4.1 — Implementar `IndeedApiClient`
- Client HTTP para o Indeed MCP connector.
- Serialização/deserialização da resposta JSON.
- **TDD:** testes com transporte mockado/fixture de resposta JSON primeiro.

Exemplo de fixture de resposta JSON do Indeed MCP:
```json
{
  "jobId": "5-cmh1-0-1jj9snbbvr8er800-358c3bd3a6b73ba5",
  "title": "Java Backend Developer | Jr (Remote)",
  "company": "Invillia",
  "location": "Remoto",
  "postedAt": "2026-03-05",
  "applyUrl": "https://to.indeed.com/aas2tpyk2v6d"
}
```

#### Story 4.2 — Implementar `IndeedJobNormalizer`
- Mapear `IndeedApiResponse` → `JobPosting` com `publishedAt`, `seniority = JUNIOR`,
  `techStackTags = "Java,Spring Boot"`.
- **TDD:** testes de normalização de campos primeiro.

#### Story 4.3 — Implementar `IndeedApiJobScraperStrategy`
- Integrar client + normalizer.
- **TDD:** testes de extração via fixture de resposta API primeiro.

#### Story 4.4 — Persistir vagas do Indeed
- Executar fluxo completo: command → strategy → normalize → persist.
- **TDD:** integration test para fatia completa primeiro.

---

### Iteration 5 — Segunda integração API-first: DOU API (Concursos Federais)

#### Story 5.1 — Implementar `DouApiClient`
- Client REST para API do Diário Oficial da União (in.gov.br/dados-abertos).
- Filtrar por palavras-chave: "Analista de TI", "Desenvolvedor", "Tecnologia da Informação".
- **TDD:** testes com transporte mockado + fixture JSON do DOU primeiro.

#### Story 5.2 — Implementar `DouContestNormalizer`
- Mapear resposta DOU → `PublicContestPosting` com `governmentLevel = FEDERAL`,
  `publishedAt`, `editalUrl`, `organizer`.
- **TDD:** testes de normalização primeiro.

#### Story 5.3 — Implementar `DouApiContestScraperStrategy`
- Integrar client + normalizer.
- **TDD:** testes de extração via fixture DOU primeiro.

#### Story 5.4 — Persistir concursos do DOU
- Executar fluxo completo para concursos federais.
- **TDD:** integration test para fatia completa de concursos primeiro.

---

### Iteration 6 — Agendamento e execução manual

#### Story 6.1 — Trigger por agendador (scheduler)
- Execução cron para jobs habilitados.
- **TDD:** testes de trigger do scheduler primeiro.

#### Story 6.2 — Endpoint de execução manual
- `POST /api/v1/crawl-jobs/{jobId}/execute`
- **TDD:** controller/use-case tests primeiro.

#### Story 6.3 — Log de execução
- Persistir status started/succeeded/failed e contadores.
- **TDD:** testes de ciclo de vida de status primeiro.

#### Story 6.4 — Endpoint de listagem de vagas por data
- `GET /api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND&seniority=JUNIOR`
- `GET /api/v1/public-contests?status=OPEN&orderBy=registrationEndDate`
- **TDD:** testes de endpoint com consulta por `publishedAt` primeiro.

---

### Iteration 13.3 — Backlog híbrido de polos tecnológicos

#### Story 13.3.1 — Priorizar polos com oferta mista de privado + público
- Formalizar backlog híbrido para expansão depois do trio municipal inicial.
- Registrar ordem de ataque:
  - `Campinas`
  - `Santa Rita do Sapucaí`
  - `Itajubá`
  - `Poços de Caldas`
  - `Extrema`
- Definir para cada cidade:
  - hipótese de fontes privadas (ATS públicos, páginas de carreira, hubs locais);
  - hipótese de fontes públicas (prefeitura, autarquias, câmaras, consórcios, IFs/universidades);
  - critério mínimo de entrada: pelo menos uma rota privada ou pública com sinal técnico/legal real.
- **TDD/documentação:** story curta de priorização antes de abrir a próxima implementação.

#### Story 13.3.2 — Campinas híbrido: abertura por trilha privada + pública oficial
- Abrir `Campinas` como primeira cidade do backlog híbrido.
- Definir as duas trilhas iniciais:
  - privada: `CI&T` via `Lever` público (`jobs.lever.co/ciandt`)
  - pública: portal oficial `Concursos e Empregos` da Prefeitura de Campinas
- Ordem recomendada de implementação:
  1. implementar onboarding privado `Lever` da CI&T
  2. validar onboarding público do portal oficial de concursos
  3. decidir se a primeira fatia executável sai:
     - só privada;
     - só pública;
     - ou mista, dependendo do menor atrito técnico
- Critério de entrada:
  - fonte privada `lever_ciandt` implementada
  - fonte pública oficial com edital/processo seletivo navegável sem autenticação

Status atual:
- trilha privada `Lever` da `CI&T` implementada
- trilha pública oficial de `Campinas` implementada via JSONAPI oficial do portal (`municipal_campinas`)
- ativação operacional/legal da trilha pública ainda pendente

#### Story 13.3.4 — Ativação operacional/legal de `municipal_campinas`
- Revisar a evidência oficial da trilha pública de `Campinas` antes de qualquer enable em produção.
- Confirmar, no mínimo:
  - `robots.txt` oficial acessível e permissivo para leitura pública;
  - JSONAPI oficial acessível sem autenticação;
  - validação real do `operational-check` com a aplicação rodando;
  - decisão explícita sobre revisão de termos/base legal.
- Critério de saída:
  - se a revisão legal fechar, promover o perfil para `APPROVED/enabled=true`;
  - se a revisão legal não fechar, manter `PENDING_REVIEW` com blocker documentado.

Status atual:
- `robots.txt` oficial respondeu `Allow: /`
- a JSONAPI oficial e o `operational-check` já foram validados tecnicamente
- a trilha privada `lever_ciandt` já opera em runtime real
- `municipal_campinas` continua `PENDING_REVIEW/enabled=false` porque a revisão final de termos/base legal ainda não foi concluída

#### Story 13.3.5 — Santa Rita do Sapucaí híbrido: abertura privada + pública oficial
- Abrir `Santa Rita do Sapucaí` como a próxima cidade do backlog híbrido depois de `Campinas`.
- Definir duas trilhas iniciais com sinal técnico real:
  - privada: `WatchGuard Technologies` via `Lever` público (`jobs.lever.co/watchguard`)
  - pública: página oficial de processos seletivos da Câmara Municipal
- Referências verificadas:
  - privada: `https://jobs.lever.co/watchguard`
  - pública: `https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025`
- Critério de entrada:
  - board privado público, sem autenticação e com vagas associadas a `Santa Rita Do Sapucai, Brazil`
  - fonte pública oficial estruturada, navegável e com edital/processo seletivo acessível

Status atual:
- trilha privada `WatchGuard` mapeada com vagas on-site e híbridas ligadas a `Santa Rita Do Sapucai, Brazil`
- trilha pública oficial da Câmara Municipal mapeada com `Processos Seletivos 2025`
- decisão tomada para a próxima implementação:
  - abrir primeiro a trilha privada `WatchGuard` via `Lever`
  - deixar a trilha pública oficial da Câmara para a fatia seguinte

#### Story 13.3.6 — Santa Rita do Sapucaí privada via `WatchGuard` no `Lever`
- Implementar a primeira fatia executável de `Santa Rita do Sapucaí` reaproveitando o pipeline
  genérico de `Lever`.
- Entrega mínima:
  - novo perfil curado `lever_watchguard`
  - catálogo operacional atualizado
  - runner validado para o novo `siteCode`
- Critério de saída:
  - `lever_watchguard` acessível via onboarding profile catalog
  - pipeline `Lever` reutilizado sem duplicação de strategy/import/normalizer
  - validação operacional real fica como próximo passo antes de ativação

Status atual:
- `lever_watchguard` implementado no catálogo curado
- runner continua roteando `lever_*` pelo `LeverJobImportUseCase`
- a validação real da aplicação para `WatchGuard` ainda é a próxima etapa

---

### Iteration 7 — Baseline de resiliência

#### Story 7.1 — Política de retry
- Adicionar Resilience4j retry em torno do fetcher.
- **TDD:** testes de exceção retryable vs non-retryable primeiro.

#### Story 7.2 — Rate limiting
- Adicionar perfil de rate-limiter por site.
- Nenhum site pode ter perfil "sem limite".
- **TDD:** testes de denial e permit primeiro.

#### Story 7.3 — Circuit breaker e dead-letter
- Abrir circuito após threshold de falha sustentada.
- Rotear falhas esgotadas para dead-letter.
- **TDD:** testes de open-state e roteamento primeiro.

---

### Iteration 8 — Primeiro scraper HTML estático: PCI Concursos

#### Story 8.1 — Capturar fixture HTML do PCI Concursos
- Salvar HTML representativo da página de listagem de concursos de TI.
- Salvar output esperado do parse (campos normalizados).
- Documentar seletores CSS identificados via inspeção + referência do projeto open source
  `luiseduardobr1/PCIConcursos` (ADR010).
- **TDD:** fixture parser test vem antes da implementação.

#### Story 8.2 — Implementar `SelectorBundle` para PCI Concursos
- Mapear campos: `contestName`, `organizer`, `numberOfVacancies`, `salaryRange`,
  `registrationDeadline`, `detailUrl`.
- Versionar bundle como `pci_concursos_v1`.
- **TDD:** testes de mapeamento de seletores primeiro.

#### Story 8.3 — Implementar `PciConcursosScraperStrategy`
- Parsing HTML via jsoup com seletores do bundle.
- Paginação: seguir links de próxima página até fim.
- **TDD:** testes de extração com fixture HTML primeiro.

#### Story 8.4 — Verificar robots.txt e ToS do PCI Concursos antes de ativar em produção
- Documentar resultado da revisão no campo `legalStatus` do `TargetSiteEntity`.
- Preencher checklist de onboarding (ADR002, Seção 3).
- **Bloqueante:** scraper não é ativado sem checklist completo.

---

### Iteration 9 — Expansão do setor privado para PMEs via ATS público (Greenhouse first)

#### Story 9.1 — Onboarding legal e seleção do primeiro board PME
- Reutilizar `TargetSiteOnboardingValidator` e `SiteOnboardingChecklist` criados na Story 8.4,
  sem criar lógica paralela de compliance.
- Fechar o onboarding de um `TargetSiteEntity` PME específico, não apenas produzir pesquisa
  documental genérica.
- Selecionar empresas-alvo com foco em Java / backend / plataforma / TI.
- Priorizar boards públicos em Greenhouse; manter Lever e Ashby no backlog imediato.
- Primeiro board recomendado: `bitso` em Greenhouse, por aderência atual ao foco Java/backend no
  board público e disponibilidade de Job Board API oficial em 2026-03-13.
- Registrar `robots.txt`, termos e endpoint público utilizado para o board escolhido.
- **TDD:** teste de metadata / onboarding validation primeiro.

#### Story 9.2 — Implementar `GreenhouseJobBoardClient`
- Consumir `boards-api.greenhouse.io`.
- Buscar vagas publicadas do board escolhido.
- **TDD:** testes com fixture JSON do Job Board API primeiro.

#### Story 9.3 — Implementar `GreenhouseJobNormalizer`
- Mapear payload do Greenhouse para `JobPostingEntity`.
- Cobrir `title`, `company`, `location`, `canonicalUrl`, `publishedAt`, `description`.
- **TDD:** testes de normalização primeiro.

#### Story 9.4 — Implementar `GreenhouseJobScraperStrategy`
- Integrar client + normalizer.
- **TDD:** testes de strategy primeiro.

#### Story 9.5 — Persistir vagas PME via Greenhouse
- Executar fluxo completo: command → strategy → normalize → persist.
- **TDD:** integration test da fatia completa primeiro.
- **Aceite manual obrigatório:** executar um job Greenhouse alinhado ao foco do usuário e validar
  retorno em listagem por data para uma pesquisa como `desenvolvedor de software em java spring boot`.
- Estado atual:
  - persistência ponta a ponta fechada;
  - boards Greenhouse ativos passaram a materializar `?content=true`;
  - reruns reenriquecem `job_postings` já existentes, evitando preservar versões pobres;
  - heurísticas de `Go` e `Python` foram endurecidas para reduzir falso positivo por texto
    genérico em vagas não técnicas.

#### Story 9.6 — Generalizar provider ATS para `LeverPostingsClient`
- Repetir o mesmo padrão em um segundo provider ATS.
- Validar se a abstração do provider permanece estável.
- **TDD:** contract test do provider e testes com fixture JSON primeiro.

---

### Iteration 10 — Processamento assíncrono

#### Story 10.1 — Abstração de fila
- Introduzir contrato de dispatch de job.
- Filas: `static-scrape-jobs`, `api-jobs`, `dynamic-browser-jobs`, `dead-letter-jobs`.
- **TDD:** testes de contrato producer/consumer primeiro.

#### Story 10.2 — Worker de execução
- Consumer resolve strategy e persiste output.
- Enquanto a fila for apenas em memória, o scheduler atua somente como producer e **não** faz
  handoff atômico/desarme do job no banco.
- Duplicações de handoff nessa fase devem ser absorvidas pela Story 10.3 (idempotência), e não por
  uma falsa garantia de durabilidade da fila em memória.
- **TDD:** testes de worker end-to-end primeiro.

#### Story 10.3 — Idempotência e prevenção de duplicatas
- Garantir que eventos repetidos não dupliquem registros.
- Cobrir explicitamente duplicatas vindas do handoff `scheduler -> in-memory queue`.
- Introduzir um `in-flight claim registry` em memória para impedir reenfileiramento do mesmo job
  recorrente dentro do mesmo processo, sem fingir durabilidade.
- Preparar o sistema para futura adoção de handoff durável (`outbox` ou fila persistida) sem mudar
  o contrato funcional dos imports.
- **TDD:** testes de execução duplicada primeiro.

#### Story 10.4 — PersistentCrawlJobQueue no Postgres
- Introduzir storage durável da fila em tabela própria no Postgres.
- Modelar estados mínimos de mensagem: `READY`, `CLAIMED`, `RETRY_WAIT`, `DEAD_LETTER`, `DONE`.
- Persistir payload materializado suficiente para reconstrução do job sem depender de memória local.
- Fechar em fatias:
  - `10.4.1` storage + migration + repository com claim atômico
  - `10.4.2` adaptador `PersistentCrawlJobQueue`
  - `10.4.3` ack/retry/dead-letter persistentes
  - `10.4.4` migração de scheduler/worker para o lifecycle persistente da fila
  - `10.4.5` revisão de simplificação e limpeza
- Estado atual após 10.4.5:
  - `PersistentCrawlJobQueue` é o caminho principal de produção
  - `InMemoryCrawlJobQueue` fica restrita a testes de contrato e cenários locais sem Spring
  - testes de uso cobrem o fluxo `scheduler -> persistent queue -> worker` para sucesso e retry
  - `InFlightCrawlJobRegistry` ainda permanece como mitigação temporária no scheduler e deve ser revisado em limpeza futura, não em stories funcionais imediatas
- **TDD:** entity/repository contract tests primeiro.

#### Gate arquitetural antes das próximas stories funcionais

Antes de seguir o roadmap funcional com segurança, o projeto precisa fechar uma story técnica de
**handoff durável** entre scheduler e worker.

Decisão explícita:

- `InMemoryCrawlJobQueue` + `InFlightCrawlJobRegistry` é apenas mitigação local por processo
- isso reduz duplicatas e looping dentro da instância atual, mas **não** resolve durabilidade após
  restart/crash
- a próxima evolução arquitetural necessária é uma destas opções:
  - fila persistida em tabela no Postgres
  - outbox pattern no Postgres
  - broker externo durável

Recomendação atual do projeto:

- priorizar **Story 10.4 — fila persistida/outbox no Postgres** antes de avançar outras stories funcionais
- só depois reconsiderar remoção de mitigadores em memória
- tratar qualquer continuação sem esse passo como evolução com risco operacional conhecido

---

### Iteration 11 — Fallback dinâmico (Playwright)

**Pré-requisito:** site classificado como Tipo C com caso falhando comprovado.

#### Story 11.1 — Contrato do adaptador Playwright
- Criar `PlaywrightJobFetcher` como implementação de `JobFetcher`.
- **TDD:** contract test com comportamento de página controlado (`WireMock` ou transporte
  equivalente + HTML fake) primeiro.
- Status atual: `PlaywrightJobFetcher` já implementa o contrato com um `PlaywrightBrowserClient` testável e um cliente padrão para Chromium; o teste unitário `PlaywrightJobFetcherTest` cobre o mapeamento do payload.

#### Story 11.2 — Strategy para site dinâmico
- Usar browser fetch somente para sites Tipo C classificados.
- **TDD:** fixture de extração dinâmica falhando primeiro.
- Status atual: `PlaywrightDynamicScraperStrategy` já está implementada, parseia cartões JS-heavy via `DynamicJobListingParser` e falha rapidamente quando o fetch retorna status diferente de 200.

#### Story 11.3 — Bulkhead para browser jobs
- Isolar concorrência de browser dos jobs estáticos.
- **TDD:** testes de limite de concorrência primeiro.
- Status atual: criada a `PlaywrightConcurrencyService` com `Semaphore`, a strategy consome o serviço e o `PlaywrightConfiguration` expõe o bean para garantir que nenhum browser extra seja aberto acima do limite configurado.

#### Story 11.4 — Verificação em campo (manual)
- Definir e executar um fluxo de aceitação manual (um `CrawlJob` parametrizado para um site Type C real) para validar que o Playwright fallback funciona em produção.
- **TDD:** documentar o cenário de uso e preparar fixture/configuração antes de aplicar o Playwright real.
- Status atual: o projeto já passou a documentar um procedimento oficial de aceite manual por família de fonte. Para a família Gupy, o fluxo validado em campo é disparar os jobs `15`, `16`, `17` e `18` pelo endpoint manual e depois consultar `job_postings` por uma intenção do usuário (`java`/`spring`/`kotlin`/`backend`/`desenvolvedor`) com recência obrigatória para verificar a utilidade real do dado persistido.

Procedimento documentado:

```bash
for id in 15 16 17 18; do
  echo -n "Job $id:"
  curl -s -X POST http://localhost:8080/api/v1/crawl-jobs/$id/execute
  echo ""
done
```

```sql
SELECT title, company, seniority, tech_stack_tags, canonical_url
FROM job_postings
WHERE published_at >= CURRENT_DATE - INTERVAL '60 days'
  AND (application_deadline IS NULL OR application_deadline >= CURRENT_DATE)
  AND (
    tech_stack_tags ILIKE '%java%'
    OR tech_stack_tags ILIKE '%spring%'
    OR tech_stack_tags ILIKE '%kotlin%'
    OR title ILIKE '%backend%'
    OR title ILIKE '%software engineer%'
    OR title ILIKE '%desenvolvedor%'
  )
ORDER BY
    CASE seniority
        WHEN 'JUNIOR' THEN 1
        WHEN 'INTERN' THEN 2
        WHEN 'MID'    THEN 3
        WHEN 'SENIOR' THEN 4
        ELSE 5
    END,
    published_at DESC;
```


---

### Iteration 12 — Observabilidade e governança

#### Story 12.1 — Métricas e logs estruturados
- Emitir contadores e durações de job.
- **TDD:** testes de emissão de métricas/logs primeiro.
- Status atual: `CrawlObservabilityService` já emite métricas Micrometer e logs estruturados para
  outcomes de dispatch (`SUCCEEDED`, `FAILED`, `DEAD_LETTER`) e worker (`done`, `retry_scheduled`,
  `dead_letter_*`, `empty`), com cobertura de testes unitários focados.

#### Story 12.2 — Checklist de habilitação de site em produção
- Estender o checklist/validator introduzido na Story 8.4, sem duplicar regras paralelas.
- Bloquear ativação sem campos de compliance preenchidos.
- **TDD:** testes de validação primeiro.
- Status atual: `SiteOnboardingChecklist` e `TargetSiteOnboardingValidator` agora são consumidos
  por `ActivateTargetSiteUseCase` e `POST /api/v1/target-sites/{siteId}/activation`; a aplicação
  persiste o estado reconciliado do `TargetSite` e responde `409 CONFLICT` quando a compliance
  bloqueia `enabled=true`.

#### Story 12.3 — Endpoint de health summary
- `GET /api/v1/scraper/health` — resumo de jobs recentes.
- **TDD:** endpoint tests primeiro.
- Status atual: endpoint implementado, retornando:
  - contagens por `CrawlExecutionStatus`;
  - contagens por fila/status da fila persistida;
  - últimas 10 execuções persistidas.

#### Story 12.4 — automação/checklist operacional de onboarding por fonte
- Expor templates operacionais de onboarding curados pela aplicação.
- Padronizar leitura de checklist por fonte antes da ativação.
- **TDD:** catálogo e endpoint tests primeiro.
- Status atual: implementado com catálogo operacional e endpoints:
  - `GET /api/v1/onboarding-profiles`
  - `GET /api/v1/onboarding-profiles/{profileKey}`
  O catálogo já cobre `greenhouse_bitso`, `indeed-br`, `dou-api` e `pci_concursos`.

#### Story 12.5 — bootstrap de novos perfis/famílias de onboarding
- Expandir o catálogo operacional para famílias/fontes reais já suportadas.
- Preservar compatibilidade do contrato v1 do payload detalhado.
- **TDD:** catálogo/controller tests primeiro.
- Status atual: implementado com perfis curados para `indeed-br`, `dou-api` e `pci_concursos`,
  além de `greenhouse_bitso`. O payload detalhado preserva `boardToken` para clientes Greenhouse.

#### Story 12.6 — bootstrap de `TargetSite` a partir de perfil curado
- Materializar um `TargetSite` persistido diretamente do catálogo operacional.
- Reduzir o passo manual entre leitura do perfil e ativação de compliance.
- **TDD:** use case/controller tests primeiro.
- Status atual: implementado com:
  - `POST /api/v1/onboarding-profiles/{profileKey}/bootstrap-target-site`
  - upsert por `siteCode`
  - `201 CREATED` para criação nova e `200 OK` para atualização
  - preservação de `enabled`, `legalStatus` e `createdAt` quando o site já existe

#### Story 12.7 — bootstrap de `CrawlJob` a partir de `TargetSite` curado
- Materializar um `CrawlJob` persistido a partir do `TargetSite` já bootstrapado.
- Eliminar o passo manual entre site persistido e job executável.
- **TDD:** use case/controller tests primeiro.
- Status atual: implementado com:
  - `POST /api/v1/target-sites/{siteId}/bootstrap-crawl-job`
  - upsert canônico por `target_site_id`
  - `201 CREATED` para criação nova e `200 OK` para atualização
  - `V009` adicionando unicidade de `crawl_jobs.target_site_id`

#### Story 12.8 — bootstrap opcional de execução inicial / smoke run operacional
- Executar um primeiro dispatch controlado da fonte recém-bootstrapada.
- Reutilizar o job canônico do site como referência, mas executar a verificação como job transitório
  one-off.
- **TDD:** use case/controller tests primeiro.
- Status atual: implementado com:
  - `POST /api/v1/target-sites/{siteId}/smoke-run`
  - bootstrap implícito do `CrawlJob` canônico
  - dispatch síncrono via `CrawlJobDispatcher` de um job transitório `schedulerManaged=false`
  - coordenação com `InFlightCrawlJobRegistry` para evitar duplicidade
  - retorno com `bootstrapStatus` + `smokeRunStatus` + `dispatchStatus`

#### Story 12.9 — orquestração unificada de onboarding por `profileKey`
- Reduzir o número de chamadas do operador no onboarding curado.
- Reaproveitar os casos de uso já estabilizados de bootstrap de `TargetSite`, bootstrap de
  `CrawlJob` e smoke run.
- **TDD:** use case/controller tests primeiro.
- Status atual: implementado com:
  - `POST /api/v1/onboarding-profiles/{profileKey}/bootstrap`
  - query param opcional `smokeRun=true|false`
  - resposta consolidada com estados de bootstrap do site, bootstrap do job e smoke run opcional
  - `201 CREATED` quando o fluxo cria `TargetSite` ou `CrawlJob`; `200 OK` quando ambos ja existiam

#### Story 12.10 — assistente de evidências de compliance
- Reduzir o preenchimento manual antes da ativação.
- Gerar draft assistido de `robots.txt`, ToS e endpoint oficial a partir de perfil curado ou do
  `TargetSite` persistido.
- **TDD:** use case/controller tests primeiro.
- Status atual: implementado com:
  - `GET /api/v1/target-sites/{siteId}/activation-assistance`
  - prefill curado quando o `siteCode` corresponde a um perfil do catálogo
  - fallback derivado de `targetSite.baseUrl` e metadados técnicos quando não há perfil curado
  - retorno de `blockingReasonsIfActivatedNow` pelo mesmo validator usado na ativação real

#### Próxima recomendação após 2026-03-25

Com fila persistida, perfis de busca, reenriquecimento Greenhouse, limpeza das heurísticas de
stack, métricas/logs, health summary, gate de ativação, catálogo operacional e bootstrap de
`TargetSite`/`CrawlJob`, smoke run inicial e orquestração unificada por `profileKey` já
estabilizados, a próxima story mais defensável passa a ser:

#### Story 12.11 — teste operacional do usuário e automação local de start-to-dispatch
- Transformar o fluxo real do usuário em um caminho reproduzível ponta a ponta.
- Consolidar bootstrap, execução observada e amostra de dados persistidos em um resumo único.
- **TDD:** use case/controller tests primeiro; script local fino por cima do endpoint consolidado.
- Status atual: implementado com:
  - `POST /api/v1/onboarding-profiles/{profileKey}/operational-check`
  - query params `smokeRun` e `daysBack`
  - resumo único com bootstrap, execução observada e amostra de vagas recentes
  - script `webscraper/scripts/run-local-operational-check.sh` para subir a app local, esperar
    `health` e executar o fluxo

#### Próxima recomendação após 2026-03-25

Com o fluxo operacional do usuário já automatizado localmente, a próxima story mais defensável
passa a ser:

- **Story 13.1 — expansão para vagas de TI em prefeituras próximas**

Razão:

- o gargalo deixa de ser operação local e passa a ser cobertura de fonte;
- a automação local já permite validar rapidamente novas integrações;
- agora faz sentido ampliar o projeto para fontes municipais/regionais de baixo risco legal.

Direção operacional desta expansão:

- tratar prefeituras como família `PUBLIC_CONTEST`, não como mercado privado;
- filtrar por cargo, escolaridade e formação exigida (`Ciência da Computação`, `Sistemas de
  Informação`, `Tecnologia da Informação`, `Análise de Sistemas`, etc.);
- não usar `seniority` (`junior/pleno/senior`) como eixo principal desta família;
- priorizar municípios com sinal de API/dados abertos antes de cair para HTML/PDF.

Inventário inicial já levantado:

- candidatos com sinal melhor de API/dados abertos:
  - `Inconfidentes`
  - `Pouso Alegre`
  - `Munhoz`
- candidatos com estratégia provável `HTML + PDF`:
  - `Ouro Fino`
  - `Andradas`
  - `Monte Sião`
  - `Poços de Caldas`
  - e o restante do inventário municipal documentado em `ADR002`

#### Story 13.1 — expansão municipal `PUBLIC_CONTEST` (primeira fatia)
- Registrar e priorizar os primeiros candidatos municipais de menor atrito técnico.
- Preservar a família municipal separada da heurística de mercado privado.
- **TDD:** catálogo/controller tests primeiro.
- Status atual: inventário e priorização formal documentados para:
  - `municipal_inconfidentes`
  - `municipal_pouso_alegre`
  - `municipal_munhoz`
- Esses candidatos **não** entram no catálogo operacional runtime ainda, porque o executor municipal
  real ainda não existe.
- Próxima fatia da 13.1:
  - validar endpoint reutilizável de dados abertos/API em cada município;
  - só depois abrir importador municipal real.

#### Story 13.2 — validação técnica dos municípios prioritários
- Verificar se `Inconfidentes`, `Pouso Alegre` e `Munhoz` expõem endpoint realmente reutilizável
  para concursos.
- Decidir API-first ou descida para `HTML + PDF` por domínio.
- **TDD:** parser/normalizer/strategy tests primeiro para o primeiro domínio escolhido.
- Status atual:
  - `Inconfidentes`: API/dados abertos existem, mas o caminho útil real para concursos ficou na
    página oficial de editais;
  - `Pouso Alegre`: API pública confirmada, sem endpoint específico de concursos confirmado;
  - `Munhoz`: API pública confirmada, sem endpoint específico de concursos confirmado.

#### Consolidação operacional municipal
- Status atual dos templates curados no catálogo runtime:
  - `municipal_inconfidentes`: `APPROVED/enabled=true`
  - `municipal_pouso_alegre`: `APPROVED/enabled=true`
  - `municipal_munhoz`: `APPROVED/enabled=true`
- Evidência operacional mínima revisada:
  - portais oficiais públicos;
  - `robots.txt` público;
  - página pública de LGPD/termo de uso revisada nos três domínios.

#### Story 13.2.1 — Inconfidentes HTML + PDF
- Implementar a primeira fonte municipal operacional por HTML oficial + links de edital/anexos.
- Modelar a fonte como `PUBLIC_CONTEST`, com foco em cargo, formação e escolaridade.
- **TDD:** parser fixture, normalizer, strategy, runner e catálogo operacional primeiro.
- Status atual: implementado com
  - parser `InconfidentesEditaisFixtureParser`;
  - normalizer `InconfidentesContestNormalizer`;
  - strategy `InconfidentesContestScraperStrategy`;
  - import use case `InconfidentesContestImportUseCase`;
  - rota operacional no `ImportingCrawlJobExecutionRunner`;
  - perfil curado operacional `municipal_inconfidentes`.

#### Story 13.2.3 — Inconfidentes PDF Enrichment Generalization
- Aprofundar o parsing do edital PDF de `Inconfidentes` sem quebrar o pipeline municipal atual.
- Preservar detalhe reutilizável para outras prefeituras:
  - múltiplos cargos detectados no PDF;
  - referências de anexos;
  - manutenção desses detalhes no `payloadJson`.
- **TDD:** parser, enricher e normalizer tests primeiro.
- Status atual: implementado com
  - metadata enriquecida em `InconfidentesEditalPdfMetadata`;
  - parser de listas de cargos e anexos em `InconfidentesEditalPdfMetadataParser`;
  - propagação no preview via `InconfidentesContestPdfEnricher` e `InconfidentesContestPreviewItem`;
  - serialização desses detalhes no `payloadJson` do concurso.

#### Story 13.2.4 — Pouso Alegre HTML + PDF
- Implementar a segunda fonte municipal via portal estruturado de concursos.
- Encadear:
  - listagem `concursos-publicos`;
  - detalhe `concursos_view/<id>`;
  - seleção do anexo principal do tipo `Edital`;
  - reaproveitamento do enrichment de PDF já usado em `Inconfidentes`.
- **TDD:** parser da listagem e detalhe primeiro, depois strategy, import e catálogo.
- Status atual: em progresso com
  - parser `PousoAlegreConcursosParser`;
  - strategy `PousoAlegreContestScraperStrategy`;
  - import use case `PousoAlegreContestImportUseCase`;
  - rota operacional no `ImportingCrawlJobExecutionRunner`;
  - perfil curado `municipal_pouso_alegre` no catálogo operacional.

---

## Definition of Done

Uma story está pronta apenas quando:

1. Testes falhando foram escritos antes do código de produção.
2. Implementação faz os testes passarem.
3. Refatoração concluída.
4. Code review realizado.
5. CI verde.
6. Fixtures e versionamento atualizados quando comportamento de extração mudou.
7. Implicações operacionais documentadas quando relevante.
8. Checklist de onboarding preenchido para qualquer novo site adicionado.
9. Cenário manual de aceite do usuário executado e documentado para a família de fonte alterada.

---

## Order of Delivery Summary

| Ordem | Feature | Razão |
|---|---|---|
| 1 | Foundation + domain model | Base obrigatória para tudo |
| 2 | Indeed MCP integration | API oficial, menor risco legal, maior valor imediato |
| 3 | DOU API integration | Dados públicos, API oficial, concursos federais |
| 4 | Scheduling + listagem por data | Entrega visível para o usuário |
| 5 | Resiliência baseline | Estabilidade antes de novos scrapers |
| 6 | PCI Concursos (HTML estático) | Site mais relevante para concursos, template para demais |
| 7 | Vagas setor privado (HTML estático) | Segundo source de scraping após validação legal |
| 8 | Processamento assíncrono | Escalabilidade após sources validados |
| 9 | Playwright fallback | Somente se Type C comprovado por cenário falhando |
| 10 | Observabilidade e governança | Operacionalização completa |

## References

- ADR001 — Direção tecnológica
- ADR002 — Taxonomia de sites e análise legal
- ADR003 — Stack Java e projetos open source
- ADR004 — Arquitetura de extração
- ADR005 — Modelo de domínio
- ADR006 — Resiliência e rate limiting
- ADR007 — TDD e quality gates
- ADR008 — Observabilidade e governança
- ADR010 — Open source e projetos GitHub
