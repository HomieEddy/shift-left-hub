package com.shiftleft.hub;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test that verifies the Testcontainers PostgreSQL (pgvector) container
 * starts, a database connection works, and the pgvector extension is available.
 * <p>This class does not use {@code @SpringBootTest} because Flyway
 * auto-configuration is not available in Spring Boot 4.x and the full
 * application context has pre-existing bean wiring dependencies.</p>
 */
@Testcontainers
class KnowledgeHubApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.0-pg16")
    );

    @Test
    void testContainerIsRunning() {
        assertThat(postgres).isNotNull();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void testDatabaseConnectionWorks() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             ResultSet rs = conn.createStatement().executeQuery("SELECT 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void testPgvectorExtensionAvailable() throws Exception {
        // Verifies the pgvector extension is available and usable in the container
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            // The vector extension must be created (available in the pgvector image)
            conn.createStatement().execute("CREATE EXTENSION IF NOT EXISTS vector");
            try (ResultSet rs = conn.createStatement().executeQuery(
                     "SELECT extname FROM pg_extension WHERE extname = 'vector'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("vector");
            }
        }
    }
}
