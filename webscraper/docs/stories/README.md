# Story Logs — WebScraper de Vagas

Cada arquivo nesta pasta documenta a implementação de uma story do plano XP (ADR009),
capturando tentativas, erros encontrados, decisões tomadas e o resultado final.

## Fluxo Oficial de Entrega

Antes de uma story ser considerada pronta, ela deve registrar ou obedecer a este fluxo:

1. task conferida contra projeto, ADRs, stories relacionadas, commits recentes e `README.md`;
2. implementação aberta por TDD;
3. testes focados aprovados;
4. validação real com a aplicação em execução;
5. review aprovada;
6. só depois `commit/push` para `main`.

Regra atual:

- não depender de Testcontainers como gate padrão da story;
- quando houver menção a Testcontainers em stories antigas, interpretar como contexto histórico;
- a validação operacional real da aplicação deve aparecer explicitamente quando a story mexer em
  runtime, parser, integração externa ou persistência.

## Convenção de arquivo

```
STORY-{iteration}.{numero}-{slug}.md
```

Exemplos:
- `STORY-1.2-enums-value-objects.md`
- `STORY-2.1-target-site-entity.md`

## Seções obrigatórias em cada story log

| Seção | Conteúdo |
|---|---|
| **Objetivo** | O que a story entrega e por que é necessária |
| **Ciclo TDD** | Sequência Red → Green → Refactor com detalhes |
| **Arquivos criados / modificados** | Lista com caminho relativo e responsabilidade |
| **Problemas encontrados** | Erros, breaking changes, comportamentos inesperados |
| **Causa raiz** | Por que o problema aconteceu |
| **Solução aplicada** | O que foi mudado para resolver |
| **Validação** | Testes automatizados executados e validação real com a aplicação rodando quando aplicável |
| **Lições aprendidas** | O que este projeto agora sabe que não sabia antes |
| **Estado final** | Compilação, testes, banco — tudo verde? |

## Índice de stories

| Story | Arquivo | Status |
|---|---|---|
| 1.2 — Enums e Value Objects | [STORY-1.2-enums-value-objects.md](STORY-1.2-enums-value-objects.md) | ✅ Concluída |
| 2.1 — TargetSiteEntity + Migration V001 | [STORY-2.1-target-site-entity.md](STORY-2.1-target-site-entity.md) | ✅ Concluída |
| 2.2 — CrawlJobEntity + CrawlExecutionEntity + Migration V002 | [STORY-2.2-crawl-job-execution-entity.md](STORY-2.2-crawl-job-execution-entity.md) | ✅ Concluída |
| 2.3 — JobPostingEntity + Deduplicação + Migration V004 | [STORY-2.3-job-posting-entity.md](STORY-2.3-job-posting-entity.md) | ✅ Concluída |
| 2.4 — PublicContestPostingEntity + Deduplicação + Migration V005 | [STORY-2.4-public-contest-posting-entity.md](STORY-2.4-public-contest-posting-entity.md) | ✅ Concluída |
| 2.5 — Baseline de persistência para RawSnapshot | [STORY-2.5-raw-snapshot-persistence-baseline.md](STORY-2.5-raw-snapshot-persistence-baseline.md) | ✅ Concluída |
| 3.1 — Contrato JobScraperStrategy | [STORY-3.1-job-scraper-strategy-contract.md](STORY-3.1-job-scraper-strategy-contract.md) | ✅ Concluída |
| 3.2 — JobScraperFactory | [STORY-3.2-job-scraper-factory.md](STORY-3.2-job-scraper-factory.md) | ✅ Concluída |
| 3.3 — JobFetcher | [STORY-3.3-job-fetcher.md](STORY-3.3-job-fetcher.md) | ✅ Concluída |
| 4.1 — IndeedApiClient | [STORY-4.1-indeed-api-client.md](STORY-4.1-indeed-api-client.md) | ✅ Concluída |
| 4.2 — IndeedJobNormalizer | [STORY-4.2-indeed-job-normalizer.md](STORY-4.2-indeed-job-normalizer.md) | ✅ Concluída |
| 4.3 — IndeedApiJobScraperStrategy | [STORY-4.3-indeed-api-job-scraper-strategy.md](STORY-4.3-indeed-api-job-scraper-strategy.md) | ✅ Concluída |
| 4.4 — Persistir vagas do Indeed | [STORY-4.4-indeed-job-import-use-case.md](STORY-4.4-indeed-job-import-use-case.md) | ✅ Concluída |
| 5.1 — DouApiClient | [STORY-5.1-dou-api-client.md](STORY-5.1-dou-api-client.md) | ✅ Concluída |
| 5.2 — DouContestNormalizer | [STORY-5.2-dou-contest-normalizer.md](STORY-5.2-dou-contest-normalizer.md) | ✅ Concluída |
| 5.3 — DouApiContestScraperStrategy | [STORY-5.3-dou-api-contest-scraper-strategy.md](STORY-5.3-dou-api-contest-scraper-strategy.md) | ✅ Concluída |
| 5.4 — Persistir concursos do DOU | [STORY-5.4-dou-contest-import-use-case.md](STORY-5.4-dou-contest-import-use-case.md) | ✅ Concluída |
| 6.1 — Trigger por agendador | [STORY-6.1-crawl-job-scheduler.md](STORY-6.1-crawl-job-scheduler.md) | ✅ Concluída |
| 6.2 — Endpoint de execução manual | [STORY-6.2-manual-crawl-job-execution-endpoint.md](STORY-6.2-manual-crawl-job-execution-endpoint.md) | ✅ Concluída |
| 6.3 — Log de execução | [STORY-6.3-crawl-execution-lifecycle.md](STORY-6.3-crawl-execution-lifecycle.md) | ✅ Concluída |
| 6.4 — Endpoints de listagem por data | [STORY-6.4-query-endpoints-for-postings-and-contests.md](STORY-6.4-query-endpoints-for-postings-and-contests.md) | ✅ Concluída |
| 7.1 — Política de retry | [STORY-7.1-fetch-retry-policy.md](STORY-7.1-fetch-retry-policy.md) | ✅ Concluída |
| 7.2 — Rate limiting | [STORY-7.2-fetch-rate-limiting.md](STORY-7.2-fetch-rate-limiting.md) | ✅ Concluída |
| 7.3 — Circuit breaker e dead-letter | [STORY-7.3-circuit-breaker-and-dead-letter.md](STORY-7.3-circuit-breaker-and-dead-letter.md) | ✅ Concluída |
| 8.1 — Fixture HTML do PCI Concursos | [STORY-8.1-pci-concursos-html-fixture.md](STORY-8.1-pci-concursos-html-fixture.md) | ✅ Concluída |
| 8.2 — SelectorBundle do PCI Concursos | [STORY-8.2-pci-concursos-selector-bundle.md](STORY-8.2-pci-concursos-selector-bundle.md) | ✅ Concluída |
| 8.3 — PciConcursosScraperStrategy | [STORY-8.3-pci-concursos-scraper-strategy.md](STORY-8.3-pci-concursos-scraper-strategy.md) | ✅ Concluída |
| 8.4 — Onboarding legal do PCI Concursos | [STORY-8.4-pci-concursos-legal-onboarding.md](STORY-8.4-pci-concursos-legal-onboarding.md) | ✅ Concluída |
| 9.1 — Onboarding legal e seleção do primeiro board PME | [STORY-9.1-greenhouse-board-onboarding.md](STORY-9.1-greenhouse-board-onboarding.md) | ✅ Concluída |
| 9.2 — GreenhouseJobBoardClient | [STORY-9.2-greenhouse-job-board-client.md](STORY-9.2-greenhouse-job-board-client.md) | ✅ Concluída |
| 9.3 — GreenhouseJobNormalizer | [STORY-9.3-greenhouse-job-normalizer.md](STORY-9.3-greenhouse-job-normalizer.md) | ✅ Concluída |
| 9.4 — GreenhouseJobScraperStrategy | [STORY-9.4-greenhouse-job-scraper-strategy.md](STORY-9.4-greenhouse-job-scraper-strategy.md) | ✅ Concluída |
| 9.5 — Persistir vagas PME via Greenhouse | [STORY-9.5-greenhouse-job-import-use-case.md](STORY-9.5-greenhouse-job-import-use-case.md) | ✅ Concluída |
| 9.6 — Generalizar provider ATS para Lever | [STORY-9.6-lever-postings-client.md](STORY-9.6-lever-postings-client.md) | ✅ Concluída |
| 10.1 — Abstração de fila | [STORY-10.1-crawl-job-queue-abstraction.md](STORY-10.1-crawl-job-queue-abstraction.md) | ✅ Concluída |
| 10.2 — Worker de execução | [STORY-10.2-crawl-job-worker.md](STORY-10.2-crawl-job-worker.md) | ✅ Concluída |
| 10.3 — Idempotência e prevenção de duplicatas | [STORY-10.3-idempotency-and-duplicate-prevention.md](STORY-10.3-idempotency-and-duplicate-prevention.md) | ✅ Concluída |
| 10.4 — PersistentCrawlJobQueue no Postgres | [STORY-10.4-persistent-crawl-job-queue.md](STORY-10.4-persistent-crawl-job-queue.md) | ✅ Concluída |
| 11.1 — PlaywrightJobFetcher | [STORY-11.1-playwright-job-fetcher.md](STORY-11.1-playwright-job-fetcher.md) | ✅ Concluída |
| 11.2 — Playwright dynamic strategy | [STORY-11.2-playwright-dynamic-strategy.md](STORY-11.2-playwright-dynamic-strategy.md) | ✅ Concluída |
| 11.3 — Playwright bulkhead | [STORY-11.3-playwright-bulkhead.md](STORY-11.3-playwright-bulkhead.md) | ✅ Concluída |
| 12.1 — Métricas e logs estruturados | [STORY-12.1-structured-metrics-and-logs.md](STORY-12.1-structured-metrics-and-logs.md) | ✅ Concluída |
| 12.2 — Checklist de habilitação de site em produção | [STORY-12.2-target-site-production-enablement-checklist.md](STORY-12.2-target-site-production-enablement-checklist.md) | ✅ Concluída |
| 12.3 — Endpoint de health summary | [STORY-12.3-scraper-health-summary-endpoint.md](STORY-12.3-scraper-health-summary-endpoint.md) | ✅ Concluída |
| 12.4 — Catálogo operacional de onboarding por fonte | [STORY-12.4-operational-onboarding-profile-catalog.md](STORY-12.4-operational-onboarding-profile-catalog.md) | ✅ Concluída |
| 13.1 — Expansão municipal PUBLIC_CONTEST: primeira abertura documental | [STORY-13.1-municipal-public-contest-expansion.md](STORY-13.1-municipal-public-contest-expansion.md) | ✅ Concluída |
| 13.2.1 — Inconfidentes HTML + PDF | [STORY-13.2.1-inconfidentes-html-pdf.md](STORY-13.2.1-inconfidentes-html-pdf.md) | ✅ Concluída |
| 13.2.3 — Inconfidentes PDF enrichment generalization | [STORY-13.2.3-inconfidentes-pdf-enrichment-generalization.md](STORY-13.2.3-inconfidentes-pdf-enrichment-generalization.md) | ✅ Concluída |
| 13.2.4 — Pouso Alegre HTML + PDF | [STORY-13.2.4-pouso-alegre-html-pdf.md](STORY-13.2.4-pouso-alegre-html-pdf.md) | ✅ Concluída |
| 13.2.5 — Munhoz HTML + PDF | [STORY-13.2.5-munhoz-html-pdf.md](STORY-13.2.5-munhoz-html-pdf.md) | ✅ Concluída |
| 13.3.1 — Priorização do backlog híbrido de polos tecnológicos | [STORY-13.3.1-hybrid-tech-hubs-prioritization.md](STORY-13.3.1-hybrid-tech-hubs-prioritization.md) | ✅ Concluída |
| 13.3.2 — Campinas híbrido: abertura privada + pública | [STORY-13.3.2-campinas-hybrid-opening.md](STORY-13.3.2-campinas-hybrid-opening.md) | ✅ Concluída |
| 13.3.3 — Campinas público oficial via JSONAPI | [STORY-13.3.3-campinas-public-official-jsonapi.md](STORY-13.3.3-campinas-public-official-jsonapi.md) | ✅ Concluída |
| 13.3.4 — Campinas ativação operacional/legal | [STORY-13.3.4-campinas-operational-activation-review.md](STORY-13.3.4-campinas-operational-activation-review.md) | ✅ Concluída |
| 13.3.5 — Santa Rita do Sapucaí híbrido: abertura privada + pública | [STORY-13.3.5-santa-rita-hybrid-opening.md](STORY-13.3.5-santa-rita-hybrid-opening.md) | ✅ Concluída |
| 13.3.6 — Santa Rita do Sapucaí privada via WatchGuard Lever | [STORY-13.3.6-santa-rita-watchguard-lever.md](STORY-13.3.6-santa-rita-watchguard-lever.md) | ✅ Concluída |
| 13.3.7 — Santa Rita do Sapucaí pública via Câmara HTML | [STORY-13.3.7-santa-rita-camara-public-html.md](STORY-13.3.7-santa-rita-camara-public-html.md) | ✅ Concluída |
| 13.3.8 — Santa Rita do Sapucaí pública: ativação operacional/legal da Câmara | [STORY-13.3.8-santa-rita-camara-activation-review.md](STORY-13.3.8-santa-rita-camara-activation-review.md) | ✅ Concluída |
| 13.3.9 — Itajubá híbrido: abertura privada + pública | [STORY-13.3.9-itajuba-hybrid-opening.md](STORY-13.3.9-itajuba-hybrid-opening.md) | ✅ Concluída |
| 13.3.10 — Plano de fechamento das cidades híbridas | [STORY-13.3.10-hybrid-cities-closure-plan.md](STORY-13.3.10-hybrid-cities-closure-plan.md) | 🚧 Aberta |
| 13.3.11 — Campinas privada: ativação da trilha CI&T Lever | [STORY-13.3.11-campinas-ciandt-activation-review.md](STORY-13.3.11-campinas-ciandt-activation-review.md) | ✅ Concluída |
| 13.3.12 — Santa Rita privada: revisão operacional/legal da trilha WatchGuard via Lever | [STORY-13.3.12-santa-rita-watchguard-activation-review.md](STORY-13.3.12-santa-rita-watchguard-activation-review.md) | ✅ Concluída |
| 13.3.13 — Itajubá híbrido: mapeamento das trilhas e ordem de execução | [STORY-13.3.13-itajuba-source-mapping.md](STORY-13.3.13-itajuba-source-mapping.md) | ✅ Concluída |
| 13.3.14 — Itajubá pública via Câmara HTML | [STORY-13.3.14-itajuba-camara-public-html.md](STORY-13.3.14-itajuba-camara-public-html.md) | 🚧 Em andamento |
