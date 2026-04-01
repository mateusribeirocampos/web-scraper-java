package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.model.RawSnapshotEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RawSnapshotRepository against local PostgreSQL (perfil dev).
 *
 * Locks the delivered raw-snapshot persistence baseline against the real Flyway migration.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("RawSnapshotRepository integration")
class RawSnapshotRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private RawSnapshotRepository rawSnapshotRepository;

    @BeforeEach
    void setUp() {
        rawSnapshotRepository.deleteAll();
    }

    @Nested
    @DisplayName("Persist and retrieve snapshots")
    class PersistTests {

        @Test
        @DisplayName("should persist a raw snapshot and assign an id")
        void shouldPersistRawSnapshot() {
            Instant now = Instant.now();
            RawSnapshotEntity snapshot = RawSnapshotEntity.builder()
                    .siteCode("pci-concursos")
                    .fetchedAt(now)
                    .responseBody("<html>...</html>")
                    .responseStatus(200)
                    .build();

            RawSnapshotEntity saved = rawSnapshotRepository.save(snapshot);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getSiteCode()).isEqualTo("pci-concursos");
            assertThat(saved.getResponseStatus()).isEqualTo(200);
            assertThat(saved.getFetchedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should persist snapshot without crawl_execution_id (nullable)")
        void shouldPersistSnapshotWithoutExecution() {
            RawSnapshotEntity snapshot = RawSnapshotEntity.builder()
                    .siteCode("dou")
                    .fetchedAt(Instant.now())
                    .responseBody("{\"items\":[]}")
                    .responseStatus(200)
                    .build();

            RawSnapshotEntity saved = rawSnapshotRepository.save(snapshot);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCrawlExecutionId()).isNull();
        }
    }

    @Nested
    @DisplayName("Query by site code")
    class QueryTests {

        @Test
        @DisplayName("should find all snapshots by site code")
        void shouldFindBySiteCode() {
            Instant now = Instant.now();
            rawSnapshotRepository.save(RawSnapshotEntity.builder()
                    .siteCode("pci-concursos").fetchedAt(now)
                    .responseBody("<html>page1</html>").responseStatus(200).build());
            rawSnapshotRepository.save(RawSnapshotEntity.builder()
                    .siteCode("pci-concursos").fetchedAt(now.plusSeconds(60))
                    .responseBody("<html>page2</html>").responseStatus(200).build());
            rawSnapshotRepository.save(RawSnapshotEntity.builder()
                    .siteCode("dou").fetchedAt(now)
                    .responseBody("{\"items\":[]}").responseStatus(200).build());

            List<RawSnapshotEntity> pciSnapshots =
                    rawSnapshotRepository.findBySiteCode("pci-concursos");

            assertThat(pciSnapshots).hasSize(2);
            assertThat(pciSnapshots).allMatch(s -> s.getSiteCode().equals("pci-concursos"));
        }

        @Test
        @DisplayName("should find snapshots by site code and response status")
        void shouldFindBySiteCodeAndResponseStatus() {
            Instant now = Instant.now();
            rawSnapshotRepository.save(RawSnapshotEntity.builder()
                    .siteCode("pci-concursos").fetchedAt(now)
                    .responseBody("<html>ok</html>").responseStatus(200).build());
            rawSnapshotRepository.save(RawSnapshotEntity.builder()
                    .siteCode("pci-concursos").fetchedAt(now.plusSeconds(30))
                    .responseBody("").responseStatus(503).build());

            List<RawSnapshotEntity> successful =
                    rawSnapshotRepository.findBySiteCodeAndResponseStatus("pci-concursos", 200);

            assertThat(successful).hasSize(1);
            assertThat(successful.get(0).getResponseStatus()).isEqualTo(200);
        }
    }
}
