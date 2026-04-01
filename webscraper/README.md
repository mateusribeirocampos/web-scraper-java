# WebScraper

Backend Java 21 + Spring Boot para coleta, normalizacao e disponibilizacao de vagas privadas e
concursos publicos, com prioridade para fontes API-first e fallback controlado para scraping HTML
ou browser somente quando permitido.

## Fluxo Oficial de Trabalho

Toda entrega deve seguir esta ordem:

1. revisar a task contra o estado atual do projeto, ADRs, stories, commits e `README.md`;
2. começar por testes seguindo XP/TDD;
3. implementar até os testes focados passarem;
4. validar também com a aplicação em execução, observando erros reais;
5. só então mandar para review;
6. só depois da review aprovada fazer `commit/push` para `main`.

Regra atual de testes:

- não usar Testcontainers como dependência obrigatória do fluxo de entrega;
- preferir fixtures, testes automatizados focados e validação real da aplicação rodando.

## Estado Atual

- Java 21 + Maven
- Persistencia JPA em Postgres
- Scheduler, trigger manual e fila persistida de execucao implementados
- Resilience4j com retry, rate limiting, bulkhead, circuit breaker e dead-letter
- Familias implementadas:
  - Indeed
  - DOU
  - PCI Concursos
  - Prefeitura de Inconfidentes (HTML + PDF oficial)
  - Greenhouse
  - Gupy
  - Playwright dinamico para sites Type C
- Boards Greenhouse onboardados usam `?content=true` e reruns reenriquecem registros antigos
  pela camada idempotente de persistencia
- Ativacao de `TargetSite` agora passa por endpoint/caso de uso dedicado que aplica o checklist de
  onboarding e bloqueia `enabled=true` sem compliance completa
- Antes da ativacao, a aplicacao agora tambem consegue gerar um draft assistido de compliance com
  evidencias curadas ou derivadas do `TargetSite`
- Perfis curados agora podem materializar `TargetSite` persistido via bootstrap REST, reduzindo o
  passo manual entre catálogo e ativação
- Perfis curados agora tambem podem orquestrar em uma chamada o bootstrap do `TargetSite`, o
  bootstrap do `CrawlJob` canônico e um smoke run opcional
- O projeto agora expõe também um `operational-check` unificado e um script local para reproduzir
  o fluxo ponta a ponta do usuário
- A expansão municipal `PUBLIC_CONTEST` agora já tem a primeira fonte operacional:
  `municipal_inconfidentes`
- O enrichment de PDF de `municipal_inconfidentes` agora também preserva detalhe reutilizável de
  edital, como múltiplos cargos e referências de anexos, para servir de base às próximas
  prefeituras
- `Pouso Alegre` entrou na `13.2.4` como próxima prefeitura com portal mais estruturado
  (`concursos-publicos` + `concursos_view/<id>`) e reaproveita o pipeline municipal de PDF
- `Munhoz` segue no backlog técnico até confirmação de endpoint municipal reaproveitável ou queda
  formal para HTML + PDF
- `Munhoz` agora também entra na `13.2.5` com portal estruturado e detalhe por `concursos_view/<id>`,
  reaproveitando o mesmo pipeline municipal de HTML + PDF já maturado em `Inconfidentes` e
  `Pouso Alegre`

## Validacao Manual Oficial

O fluxo manual oficial atual para validar a familia Gupy e a utilidade real dos dados persistidos e:

1. disparar os `CrawlJob`s persistidos pelo endpoint manual;
2. deixar a aplicacao concluir o dispatch/import;
3. consultar apenas vagas recentes com uma intencao de busca reconhecivel pelo usuario.

### Disparo dos jobs

```bash
for id in 15 16 17 18; do
  echo -n "Job $id:"
  curl -s -X POST http://localhost:8080/api/v1/crawl-jobs/$id/execute
  echo ""
done
```

O retorno `{\"jobId\":...,\"status\":\"DISPATCHED\"}` confirma apenas que a execucao foi aceita e
encaminhada. A verificacao funcional acontece na leitura posterior do banco.

### Consulta funcional no banco

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

Essa consulta valida ponta a ponta o caminho real de uso:

- `CrawlJob` persistido
- dispatch da execucao
- import em `job_postings`
- leitura final por uma intencao de busca do usuario

Consulta oficial pela API:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_JUNIOR_BACKEND"
```

Nesse perfil oficial, a API exclui banco de talentos, corta senioridade `SENIOR/LEAD`, bloqueia
cargos de gestao/lideranca e exige sinal real de stack (`Java`, `Spring` ou `Kotlin`) junto com
um sinal de funcao aderente (`backend`, `developer`, `software engineer`, `desenvolvedor`, etc.).

Para um recorte intermediario, que preserva stack aderente mas abre mais do que o perfil default:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_BACKEND_BALANCED"
```

Para um recorte pragmatico, focado em volume real com stack aderente e sem banco de talentos:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=JAVA_STACK_PRAGMATIC"
```

Para investigacao mais ampla da base, sem o perfil estrito:

```bash
curl "http://localhost:8080/api/v1/job-postings?category=PRIVATE_SECTOR&daysBack=60&profile=UNFILTERED"
```

Resumo operacional da fila persistida e das execucoes recentes:

```bash
curl "http://localhost:8080/api/v1/scraper/health"
```

Gate operacional de ativacao de site:

```bash
curl "http://localhost:8080/api/v1/target-sites/7/activation-assistance"

curl -X POST "http://localhost:8080/api/v1/target-sites/7/activation" \
  -H "Content-Type: application/json" \
  -d '{
    "robotsTxtUrl": "https://boards.greenhouse.io/robots.txt",
    "robotsTxtReviewed": true,
    "robotsTxtAllowsScraping": true,
    "termsOfServiceUrl": "",
    "termsReviewed": true,
    "termsAllowScraping": true,
    "officialApiChecked": true,
    "officialApiEndpointUrl": "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
    "strategySupportVerified": true,
    "businessJustification": "Private-sector Java/backend source.",
    "rateLimitProfile": "60 rpm conservative",
    "legalCategory": "API_OFICIAL",
    "owner": "platform-team@local",
    "authenticationStatus": "PUBLIC_ANONYMOUS",
    "discoveryEvidence": "Greenhouse public API reviewed."
  }'
```

O endpoint de `activation-assistance` nao ativa o site. Ele devolve um draft assistido com:

- evidencias curadas quando o `siteCode` bate com um perfil conhecido;
- evidencias derivadas de `targetSite.baseUrl` e metadados tecnicos quando nao ha perfil curado;
- `blockingReasonsIfActivatedNow`, calculadas pelo mesmo validator do gate de ativacao.

Catalogo operacional de perfis de onboarding:

```bash
curl "http://localhost:8080/api/v1/onboarding-profiles"
curl "http://localhost:8080/api/v1/onboarding-profiles/greenhouse_bitso"
curl -X POST "http://localhost:8080/api/v1/onboarding-profiles/greenhouse_bitso/bootstrap"
curl -X POST "http://localhost:8080/api/v1/onboarding-profiles/greenhouse_bitso/bootstrap?smokeRun=true"
curl -X POST "http://localhost:8080/api/v1/onboarding-profiles/greenhouse_bitso/bootstrap-target-site"
```

Esse bootstrap cria ou atualiza o `TargetSite` pelo `siteCode` do perfil curado e devolve o
`siteId` persistido para o próximo passo de ativação.

O endpoint unificado `/bootstrap` reduz o número de chamadas operacionais:

- sem `smokeRun`, ele cria/atualiza `TargetSite` e `CrawlJob` canônico;
- com `smokeRun=true`, ele também dispara a verificação one-off e devolve `smokeRunStatus` e
  `smokeRunDispatchStatus` no mesmo payload.

Para a expansão municipal de concursos:

- `municipal_inconfidentes` já entra como fonte runtime de `PUBLIC_CONTEST`;
- o filtro principal continua sendo cargo, escolaridade e formação exigida em edital, e não
  `seniority`;
- `Pouso Alegre` e `Munhoz` permanecem em avaliação técnica.

Check operacional ponta a ponta por `profileKey`:

```bash
curl -X POST "http://localhost:8080/api/v1/onboarding-profiles/greenhouse_bitso/operational-check?smokeRun=true&daysBack=60"
```

Esse endpoint consolida:

- bootstrap do `TargetSite`;
- bootstrap do `CrawlJob`;
- smoke run opcional;
- última execução observada do job disparado;
- contagem e amostra de vagas recentes persistidas para o `TargetSite`.

Script local para reproduzir o fluxo do usuário:

```bash
cd webscraper
./scripts/run-local-operational-check.sh
```

Variáveis úteis:

- `PROFILE_KEY=greenhouse_bitso`
- `SMOKE_RUN=true`
- `DAYS_BACK=60`
- `JOB_POSTINGS_PROFILE=JAVA_JUNIOR_BACKEND`
- `JOB_POSTINGS_SENIORITY=` para deixar o recorte em `junior + pleno`
- `KEEP_APP_RUNNING=true`

O script agora fecha o fluxo ponta a ponta em duas visões:

- check operacional do onboarding/bootstrap/execução;
- consulta funcional via `/api/v1/job-postings`.

Para o teste real do usuário, o default usa `JAVA_JUNIOR_BACKEND` sem `seniority` explícito. Na
prática isso amplia o recorte para `junior + pleno`, sem abrir para `senior/lead`. Se precisar do
filtro estrito:

```bash
JOB_POSTINGS_SENIORITY=JUNIOR ./scripts/run-local-operational-check.sh
```

Bootstrap do `CrawlJob` canônico a partir do `TargetSite` persistido:

```bash
curl -X POST "http://localhost:8080/api/v1/target-sites/7/bootstrap-crawl-job"
```

Esse passo cria ou atualiza o `CrawlJob` operacional do site, reduzindo o setup manual antes da
execução real.

Smoke run operacional do `TargetSite`:

```bash
curl -X POST "http://localhost:8080/api/v1/target-sites/7/smoke-run"
```

Esse endpoint reutiliza o bootstrap do job canônico, coordena com o guard de jobs em voo e
materializa um job transitório one-off para a verificação. Assim, devolve `smokeRunStatus`
(`DISPATCHED` ou `SKIPPED_IN_FLIGHT`) junto com o `dispatchStatus` quando a execução realmente foi
disparada, sem atrasar a agenda canônica do site.

## Notas de Qualidade de Busca

- `tech_stack_tags` e deliberadamente conservador para reduzir falso positivo.
- `Go` e `Python` nao sao marcados apenas por palavra solta; o normalizer exige titulo/contexto
  tecnico suficiente.
- Isso melhora a utilidade dos perfis `JAVA_JUNIOR_BACKEND`, `JAVA_BACKEND_BALANCED` e
  `JAVA_STACK_PRAGMATIC`.

## Documentacao Relacionada

- `../ADRs-web-scraping-vagas/ADR-Summary-of-WebScraper-ADRs.md`
- `../ADRs-web-scraping-vagas/ADR002-Target-Site-Taxonomy-and-Requirements-for-WebScraper.md`
- `../ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `docs/stories/README.md`
