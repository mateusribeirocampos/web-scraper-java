# Story 11.1 — PlaywrightJobFetcher

**Status:** ✅ Concluída  
**Iteration:** 11 — Fallback dinâmico (Playwright)  
**Data:** 2026-03-17  
**Referência ADR:** ADR009 Story 11.1

---

## Objetivo

- Introduzir `PlaywrightJobFetcher`, a implementação de `JobFetcher` que executa páginas JS-heavy com o browser Chromium (através do driver oficial do Playwright) e expõe o mesmo contrato funcional usado pelos demais fetchers.
- Manter o código testável ao isolar as dependências diretamente ligadas ao Playwright.

## Ciclo TDD

### RED

- Escrevemos `PlaywrightJobFetcherTest` primeiro, usando um stub do novo `PlaywrightBrowserClient` para confirmar o mapeamento do payload e a conversão de exceções em `RetryableFetchException`.

### GREEN

- Implementamos `PlaywrightJobFetcher`, `PlaywrightBrowserClient` e o cliente padrão `DefaultPlaywrightBrowserClient`, que orquestram `Playwright.create()`, lançam Chromium headless e coletam `url`, `content`, `status` e `content-type`.
- O fetcher injeta um `Clock` para permitir assertions determinísticas do timestamp (`FetchedPage.fetchedAt`).

### REFACTOR

- Separação da lógica de browser/driver em `PlaywrightBrowserClient` torna o fetcher testável sem abrir um browser real.
- Documentamos o novo contrato e registramos os testes de uso na Story Log e README.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/infrastructure/http/PlaywrightBrowserClient.java`
- `src/main/java/com/campos/webscraper/infrastructure/http/DefaultPlaywrightBrowserClient.java`
- `src/main/java/com/campos/webscraper/infrastructure/http/PlaywrightJobFetcher.java`
- `src/test/java/com/campos/webscraper/infrastructure/http/PlaywrightJobFetcherTest.java`
- `docs/stories/STORY-11.1-playwright-job-fetcher.md`
- `docs/stories/README.md` (lista de stories)

## Problemas encontrados

- O Playwright oficial não traz tipos de `LoadState` no classpath usado pela nossa versão (1.49), então optamos por não usar essa enumeração no `navigate`.
- A abertura de browsers reais ainda será validada nas stories seguintes; aqui concentramos nos contratos.

## Causa raiz

- Sites Tipo C exigem execução de JavaScript completa; o stack atual suportava apenas HTTP/JSON estático.

## Solução aplicada

- Criamos o fetcher Playwright que atende o mesmo contrato (implementa `fetch(FetchRequest)` e retorna `FetchedPage`), mas ora usa um driver de browser com headless Chromium.
- A introdução de `PlaywrightBrowserClient` permite trocar a implementação real por stubs nos testes e prepara o caminho para uma eventual fábrica de sessões mais sofisticada.

## Lições aprendidas

- Sempre encapsular bibliotecas pesadas (Playwright, browsers) atrás de interfaces com payloads simples para manter testes rápidos no nível unitário.
- O `Clock` injetado torna os testes determinísticos mesmo quando o código depende de timestamps.

## Estado final

- Testes: `./mvnw test -DexcludedGroups=integration -Dtest=PlaywrightJobFetcherTest`
- Build completo não foi disparado aqui (confiaremos no pipeline da story 10.4 para o resto).

## Próxima fatia planejada

- Story 11.2 — Strategy para sites dinâmicos (usará este fetcher e terá fixtures JS).
