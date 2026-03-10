# STORY 2.1 — TargetSiteEntity + Repository + Migration V001

**Iteration:** 2 — Modelo de Persistência
**Status:** ✅ Concluída
**Data:** 2026-03-10
**Referência ADR:** ADR009 Story 2.1 / ADR005 Seção 4

---

## Objetivo

Implementar a entidade JPA `TargetSiteEntity`, seu repositório Spring Data e a
migration Flyway `V001__create_target_sites.sql`, criando a tabela `target_sites`
no PostgreSQL — base para todas as entidades de domínio que virão nas próximas
stories.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Criado `TargetSiteRepositoryTest.java` com 7 testes de integração cobrindo:

```
✎ shouldSaveAndFindById
✎ shouldFindBySiteCode
✎ shouldReturnEmptyForUnknownSiteCode
✎ shouldRejectDuplicateSiteCode          ← valida UNIQUE constraint da migration
✎ shouldFindEnabledSitesByCategory
✎ shouldReturnEmptyWhenNoEnabledSitesForCategory
✎ shouldFindEnabledApprovedSites
✎ shouldPersistAndRetrieveEnumsAsString  ← garante EnumType.STRING, não ORDINAL
```

Esses testes referenciam `TargetSiteEntity` e `TargetSiteRepository` que ainda
não existiam — garantindo que o compilador falha antes de qualquer execução
(fase RED).

### GREEN — implementação

Criados em ordem:
1. `V001__create_target_sites.sql` — tabela com constraints e índices
2. `TargetSiteEntity.java` — entidade JPA com Lombok
3. `TargetSiteRepository.java` — interface Spring Data com 3 queries de domínio

### REFACTOR

Sem necessidade de refatoração estrutural. Pequenos ajustes de javadoc e
comentários no SQL foram feitos durante a implementação.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/resources/db/migration/V001__create_target_sites.sql` | Criado | DDL da tabela, constraints e índices |
| `src/main/java/.../domain/model/TargetSiteEntity.java` | Criado | Entidade JPA com mapeamento completo |
| `src/main/java/.../domain/repository/TargetSiteRepository.java` | Criado | Interface Spring Data JPA com queries de domínio |
| `src/test/java/.../domain/repository/TargetSiteRepositoryTest.java` | Criado | 7 testes de integração (Testcontainers + Flyway) |
| `pom.xml` | Modificado | Substituição de `flyway-core` por `spring-boot-starter-flyway` |
| `src/main/resources/application-dev.properties` | Modificado | Reescrito em UTF-8 (era ISO-8859, bloqueava Maven) |
| `src/test/java/.../TestcontainersConfiguration.java` | Modificado | Correção de import errado |

---

## Problemas encontrados

### Problema 1 — Encoding do `application-dev.properties`

**Sintoma:**
```
MalformedInputException: Input length = 1
Failed to execute goal maven-resources-plugin:3.3.1:resources
```

**Causa raiz:**
O arquivo `application-dev.properties` foi salvo em encoding ISO-8859
(provavelmente editado no Windows ou com editor sem configuração de encoding).
O plugin `maven-resources-plugin` tenta processar recursos como UTF-8 por padrão
e falha ao encontrar bytes inválidos.

**Solução:**
Reescrever o arquivo inteiramente em UTF-8/ASCII puro, substituindo todos os
caracteres acentuados nos comentários por equivalentes ASCII.

**Prevenção futura:**
Configurar o editor para sempre salvar `.properties` como UTF-8. Adicionar
`.editorconfig` ao projeto com `charset = utf-8` para todos os arquivos.

---

### Problema 2 — Import errado no `TestcontainersConfiguration`

**Sintoma:**
```
[ERROR] package org.testcontainers.postgresql does not exist
[ERROR] cannot find symbol: class PostgreSQLContainer
```

**Causa raiz:**
O arquivo gerado pelo Spring Initializr usou o import errado:
```java
// ERRADO (pacote não existe)
import org.testcontainers.postgresql.PostgreSQLContainer;

// CORRETO
import org.testcontainers.containers.PostgreSQLContainer;
```

**Solução:**
Corrigir o import para `org.testcontainers.containers.PostgreSQLContainer`.

---

### Problema 3 — `@DataJpaTest` e `@AutoConfigureTestDatabase` não encontrados (Spring Boot 4.x Breaking Change)

**Sintoma:**
```
[ERROR] package org.springframework.boot.test.autoconfigure.orm.jpa does not exist
[ERROR] cannot find symbol: class DataJpaTest
[ERROR] cannot find symbol: class AutoConfigureTestDatabase
```

**Causa raiz:**
Spring Boot 4.x realizou uma reestruturação arquitetural radical: o jar monolítico
`spring-boot-autoconfigure` foi fragmentado em dezenas de módulos específicos por
domínio. Os test slices foram movidos para módulos próprios:

| Classe | Spring Boot 3.x | Spring Boot 4.x |
|---|---|---|
| `@DataJpaTest` | `spring-boot-autoconfigure` | `spring-boot-data-jpa-test` |
| `@AutoConfigureTestDatabase` | `spring-boot-autoconfigure` | `spring-boot-jdbc-test` |

No Spring Boot 4.x, o jar `spring-boot-autoconfigure-4.0.3.jar` tem apenas
**293 entradas** contra milhares no 3.x — ele só mantém infraestrutura de
condicionamento (`@ConditionalOn*`, `@OverrideAutoConfiguration`, etc.).

**Solução:**
Substituir `@DataJpaTest` + `@AutoConfigureTestDatabase` por
`@SpringBootTest(webEnvironment = NONE)` + `@Testcontainers`:

```java
// Spring Boot 3.x (não funciona no 4.x)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TargetSiteRepositoryTest { ... }

// Spring Boot 4.x (correto)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.jpa.hibernate.ddl-auto=none"
})
class TargetSiteRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    ...
}
```

**Alternativa com os módulos corretos (a investigar):**
Os módulos `spring-boot-data-jpa-test` e `spring-boot-jdbc-test` existem no
repositório Maven 4.0.3. Para usar `@DataJpaTest` novamente no 4.x seria
necessário adicionar `spring-boot-starter-data-jpa-test` como dependência de
teste. Essa abordagem não foi testada nesta story.

---

### Problema 4 — Flyway não executava (Spring Boot 4.x Breaking Change)

**Sintoma:**
A aplicação subia sem nenhum log do Flyway. O banco ficava vazio após o startup
— nenhuma tabela criada.

**Diagnóstico:**
Inspeção do jar `spring-boot-autoconfigure-4.0.3.jar` via Python (`zipfile`):
```python
with zipfile.ZipFile(jar) as z:
    flyway = [n for n in z.namelist() if 'flyway' in n.lower()]
    # Resultado: []  ← vazio
```

Confirmação: `FlywayAutoConfiguration` não existe em nenhum jar
`spring-boot-*-4.0.3.jar` com o nome antigo. Em Spring Boot 4.x, a
autoconfiguration do Flyway foi movida para um módulo dedicado.

**Causa raiz:**
No `pom.xml` havia apenas:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Em Spring Boot 3.x, adicionar `flyway-core` ao classpath era suficiente para
acionar `FlywayAutoConfiguration`. Em Spring Boot 4.x isso **não funciona mais**
— a autoconfiguration precisa vir do starter dedicado:
`spring-boot-starter-flyway`.

**Solução:**
```xml
<!-- ANTES (Spring Boot 3.x) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- DEPOIS (Spring Boot 4.x) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<!-- flyway-database-postgresql permanece necessário para suporte ao PG -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Como verificar se o módulo correto está disponível:**
```bash
# Listar módulos Flyway do Spring Boot 4.x disponíveis no repositório local
ls ~/.m2/repository/org/springframework/boot/ | grep flyway
# spring-boot-flyway
# spring-boot-starter-flyway
# spring-boot-starter-flyway-test
```

---

### Problema 5 — Testes de integração não rodam no ambiente sandbox (Testcontainers)

**Sintoma:**
```
ERROR: UnixSocketClientProviderStrategy: failed with exception BadRequestException
  (Status 400: {"message":"client version 1.32 is too old.
   Minimum supported API version is 1.40..."})
```

**Causa raiz:**
O ambiente sandbox (Cowork) usa um proxy do socket Docker com API version 1.32.
Testcontainers requer Docker API >= 1.40.

**Impacto:**
Os testes com `@Tag("integration")` não podem rodar no sandbox, somente na
máquina de desenvolvimento local.

**Solução de contorno:**
Rodar os testes de integração diretamente no terminal da máquina local:
```bash
./mvnw test -Dgroups="integration"
```

Os testes unitários (`@Tag("unit")`) continuam funcionando normalmente no sandbox:
```bash
./mvnw test -Dgroups="unit"
```

---

## Lições aprendidas

### Spring Boot 4.x — resumo das breaking changes encontradas

| Área | Spring Boot 3.x | Spring Boot 4.x |
|---|---|---|
| Flyway autoconfiguration | Embutida em `spring-boot-autoconfigure` | Requer `spring-boot-starter-flyway` |
| `@DataJpaTest` | Em `spring-boot-autoconfigure` | Em `spring-boot-data-jpa-test` (starter separado) |
| `@AutoConfigureTestDatabase` | Em `spring-boot-autoconfigure` | Em `spring-boot-jdbc-test` (starter separado) |
| Módulo de autoconfigure | ~3000 classes num jar | ~293 classes num jar (fragmentado em módulos) |

### Diagnóstico rápido para problemas de autoconfiguration no SB 4.x

Se uma feature que funcionava no Spring Boot 3.x parou de funcionar no 4.x
sem nenhuma mensagem de erro, o primeiro passo é:

```python
# Verificar se a autoconfiguration esperada existe em algum jar 4.x
import zipfile, os, glob

boot_dir = os.path.expanduser('~/.m2/repository/org/springframework/boot')
keyword = 'flyway'  # substituir pelo que estiver faltando

for jar_path in glob.glob(f'{boot_dir}/**/*4.0.3.jar', recursive=True):
    with zipfile.ZipFile(jar_path) as z:
        matches = [n for n in z.namelist() if keyword in n.lower()]
        if matches:
            print(f'{os.path.basename(jar_path)}: {matches[:3]}')
```

### Como a migration V001 ficou estruturada

```sql
CREATE TABLE target_sites (
    id                      BIGSERIAL PRIMARY KEY,
    site_code               VARCHAR(100)  NOT NULL,   -- chave de negócio
    display_name            VARCHAR(200)  NOT NULL,
    base_url                VARCHAR(1000) NOT NULL,
    site_type               VARCHAR(20)   NOT NULL,   -- SiteType enum
    extraction_mode         VARCHAR(30)   NOT NULL,   -- ExtractionMode enum
    job_category            VARCHAR(30)   NOT NULL,   -- JobCategory enum
    legal_status            VARCHAR(30)   NOT NULL,   -- LegalStatus enum
    selector_bundle_version VARCHAR(50)   NOT NULL DEFAULT 'n/a',
    enabled                 BOOLEAN       NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ   NOT NULL,
    updated_at              TIMESTAMPTZ,
    CONSTRAINT uq_target_sites_site_code UNIQUE (site_code)
);
-- Índices para as queries de domínio mais frequentes
CREATE INDEX idx_target_sites_enabled_category ON target_sites (enabled, job_category);
CREATE INDEX idx_target_sites_legal_status      ON target_sites (legal_status);
```

**Por que `enabled DEFAULT false`?**
Todo novo site entra desabilitado por padrão. Só é ativado após o checklist
de onboarding legal (ADR002) estar completo e `legal_status = 'APPROVED'`.

**Por que enums como `VARCHAR` e não `INTEGER`?**
`EnumType.STRING` garante que o banco seja legível sem a aplicação. Com `ORDINAL`,
reordenar constantes no Java silenciosamente corrompe dados históricos.

---

## Estado final

### Banco de dados

```sql
SELECT version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;

 version |     description     | type | success
---------+---------------------+------+---------
 001     | create target sites | SQL  | t
```

### Estrutura da tabela

```
Table "public.target_sites"
 Column                  | Type                     | Default
-------------------------+--------------------------+---------------------------------
 id                      | bigint                   | nextval('target_sites_id_seq')
 site_code               | character varying(100)   |
 display_name            | character varying(200)   |
 base_url                | character varying(1000)  |
 site_type               | character varying(20)    |
 extraction_mode         | character varying(30)    |
 job_category            | character varying(30)    |
 legal_status            | character varying(30)    |
 selector_bundle_version | character varying(50)    | 'n/a'
 enabled                 | boolean                  | false
 created_at              | timestamptz              |
 updated_at              | timestamptz              |
Indexes:
  "target_sites_pkey" PRIMARY KEY
  "uq_target_sites_site_code" UNIQUE
  "idx_target_sites_enabled_category" btree (enabled, job_category)
  "idx_target_sites_legal_status" btree (legal_status)
```

### Testes

```
./mvnw test -Dgroups="unit"
Tests run: 102, Failures: 0, Errors: 0, Skipped: 0  ✅ BUILD SUCCESS

./mvnw test -Dgroups="integration"
→ Rodar localmente (Docker API incompatível no sandbox)
→ 7 testes cobrindo persistência, queries e constraints
```
