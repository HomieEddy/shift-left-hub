package com.shiftleft.hub.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the consolidated V1–V3 migration chain.
 * <p>Verifies that:</p>
 * <ol>
 *   <li>Applying V1 + V2 + V3 to a fresh database produces the full schema
 *       (all tables, triggers, and indexes from the historical V1–V12 chain).</li>
 *   <li>Re-running Flyway on the same database is a no-op (no checksum
 *       failures, no new rows in flyway_schema_history).</li>
 * </ol>
 * <p>Uses Flyway directly so the history table is populated as Spring Boot
 * would populate it on startup.</p>
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MigrationConsolidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.0-pg16")
    );

    private Flyway flyway() {
        return Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .placeholders(java.util.Map.of("embeddingDimensions", "768"))
            .load();
    }

    @Test
    @Order(1)
    void freshDatabaseAppliesAllMigrationsAndCreatesFullSchema() {
        flyway().migrate();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = conn.createStatement()) {

            try (ResultSet rs = st.executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                  + "WHERE table_schema = 'public' ORDER BY table_name")) {
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
                assertThat(tables)
                    .as("Consolidated V1 must create all expected tables")
                    .contains(
                        "app_user", "used_refresh_token", "workspace",
                        "workspace_member", "workspace_invitation",
                        "category", "tag", "article", "article_tag",
                        "ticket", "ticket_number_sequence", "work_note",
                        "document", "document_chunk", "ai_config",
                        "workspace_llm_config", "vector_store");
            }

            try (ResultSet rs = st.executeQuery(
                    "SELECT trigger_name FROM information_schema.triggers "
                  + "WHERE trigger_schema = 'public'")) {
                List<String> triggers = new ArrayList<>();
                while (rs.next()) {
                    triggers.add(rs.getString(1));
                }
                assertThat(triggers)
                    .as("V1 baseline must create tsvector triggers")
                    .contains("trigger_article_tsv", "trigger_document_chunk_tsv");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    void reRunningFlywayIsNoOp() {
        flyway().migrate();

        int firstRunRows;
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM flyway_schema_history")) {
            rs.next();
            firstRunRows = rs.getInt(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        flyway().migrate();

        int secondRunRows;
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM flyway_schema_history")) {
            rs.next();
            secondRunRows = rs.getInt(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(secondRunRows)
            .as("Re-running Flyway must not add new history rows")
            .isEqualTo(firstRunRows);
    }

    @Test
    @Order(3)
    void v2RewritesPreConsolidationHistory() throws Exception {
        // Simulate a database that ran the historical V1–V12 chain before
        // the consolidation landed. We insert fake history rows directly and
        // then run V2 to confirm it removes the obsolete rows.
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = conn.createStatement()) {

            st.execute("CREATE TABLE IF NOT EXISTS flyway_schema_history ("
                + " installed_rank INT NOT NULL, "
                + " version VARCHAR(50), "
                + " description VARCHAR(200), "
                + " type VARCHAR(20) NOT NULL, "
                + " script VARCHAR(1000) NOT NULL, "
                + " checksum INT, "
                + " installed_by VARCHAR(100) NOT NULL, "
                + " installed_on TIMESTAMP NOT NULL DEFAULT now(), "
                + " execution_time INT NOT NULL, "
                + " success SMALLINT NOT NULL, "
                + " PRIMARY KEY (installed_rank))");

            String[] versions = {"1","2","3","4","5","6","7","8","9","10","11","12"};
            int rank = 100;
            for (String v : versions) {
                st.execute(String.format(
                    "INSERT INTO flyway_schema_history "
                  + "(installed_rank, version, description, type, script, checksum, "
                  + " installed_by, installed_on, execution_time, success) "
                  + "VALUES (%d, '%s', 'historical V%s', 'SQL', 'V%s__legacy.sql', 12345, "
                  + " 'test', now(), 100, true)",
                    rank++, v, v, v));
            }

            org.springframework.core.io.ClassPathResource v2 = new org.springframework.core.io.ClassPathResource(
                "db/migration/V2__consolidate_history.sql");
            try (java.io.InputStream in = v2.getInputStream();
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
                 java.sql.Statement stmt = conn.createStatement()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                stmt.execute(sb.toString());
            }

            try (ResultSet rs = st.executeQuery(
                    "SELECT count(*) FROM flyway_schema_history "
                  + "WHERE version IN ('2','3','4','5','6','7','8','9','10','11','12')")) {
                rs.next();
                assertThat(rs.getInt(1))
                    .as("V2 must remove obsolete V2–V12 history rows")
                    .isEqualTo(0);
            }

            try (ResultSet rs = st.executeQuery(
                    "SELECT count(*) FROM flyway_schema_history "
                  + "WHERE version = '1' AND checksum IS NULL")) {
                rs.next();
                assertThat(rs.getInt(1))
                    .as("V2 must clear the V1 row's checksum "
                      + "(at least the original V1 row from Flyway, "
                      + "plus the simulated historical V1 row)")
                    .isGreaterThanOrEqualTo(1);
            }
        }
    }
}
