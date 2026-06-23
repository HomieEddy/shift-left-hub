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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    private static String v4TagTestUniqueName;

    @Test
    @Order(4)
    void v4TagDedupRemapsArticleTagBeforeDeleting() throws Exception {
        // Simulate a database that ran V1..V3 with a pre-existing duplicate tag
        // (workspace_id, name_en) that has an article_tag row referencing the
        // duplicate. The V4 unique-constraint block must remap the article_tag
        // row to the canonical (lowest-id) tag before deleting the duplicate —
        // otherwise the DELETE would fail with an FK violation.

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = conn.createStatement()) {

            UUID wsId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            UUID articleId = UUID.randomUUID();
            UUID dupTagId = UUID.randomUUID();
            UUID canonicalTagId = UUID.randomUUID();

            st.execute(String.format(
                "INSERT INTO app_user (id, email, password, display_name, role, enabled, created_at, updated_at) "
              + "VALUES ('%s', 'a@b', 'x', 'Admin', 'ADMIN', true, now(), now())", adminId));
            st.execute(String.format(
                "INSERT INTO workspace (id, name, slug, created_by, created_at, updated_at) "
              + "VALUES ('%s', 'ws', 'ws-%s', '%s', now(), now())", wsId, wsId, adminId));
            st.execute(String.format(
                "INSERT INTO article (id, title_en, content_en, status, view_count, "
              + "  author_id, workspace_id, created_at, updated_at) "
              + "VALUES ('%s', 't', 'c', 'PUBLISHED', 0, '%s', '%s', now(), now())",
                articleId, adminId, wsId));

            // Temporarily drop the constraint (added by test 1) so we can
            // insert the duplicate tags. V4 will re-add it after the dedup.
            st.execute("ALTER TABLE tag DROP CONSTRAINT IF EXISTS uc_tag_workspace_name_en");
            st.execute("ALTER TABLE category DROP CONSTRAINT IF EXISTS uc_category_workspace_parent_name_en");

            // Two tags with the same (workspace_id, name_en) — canonical is
            // inserted FIRST so its id sorts lower (UUID ordering is random,
            // but we explicitly force a tie-break with a deterministic id).
            // Use a unique name per test run to avoid colliding with any
            // existing rows from previous test methods (PostgreSQL container
            // is shared across the test class).
            v4TagTestUniqueName = "urgent-" + UUID.randomUUID();
            st.execute(String.format(
                "INSERT INTO tag (id, name_en, name_fr, color, workspace_id, created_at) "
              + "VALUES ('%s', '%s', '%s', '#f00', '%s', now())",
                canonicalTagId, v4TagTestUniqueName, v4TagTestUniqueName, wsId));
            st.execute(String.format(
                "INSERT INTO tag (id, name_en, name_fr, color, workspace_id, created_at) "
              + "VALUES ('%s', '%s', '%s', '#f00', '%s', now())",
                dupTagId, v4TagTestUniqueName, v4TagTestUniqueName, wsId));
            // article_tag points to the DUPLICATE — V4 must remap this to canonical
            st.execute(String.format(
                "INSERT INTO article_tag (article_id, tag_id) "
              + "VALUES ('%s', '%s')", articleId, dupTagId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Apply V4 — the dedup+remap must succeed
        org.springframework.core.io.ClassPathResource v4 = new org.springframework.core.io.ClassPathResource(
            "db/migration/V4__tier12_db_jpa_hygiene.sql");
        try (java.io.InputStream in = v4.getInputStream();
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in));
             Connection conn = DriverManager.getConnection(
                 postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            stmt.execute(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("V4 failed — likely the article_tag remap is missing or broken", e);
        }

        // After V4:
        //   - only one tag remains (canonical)
        //   - article_tag points to the canonical id
        //   - the unique constraint is in place
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = conn.createStatement()) {

            int tagCount;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT count(*) FROM tag WHERE name_en = ?")) {
                ps.setString(1, v4TagTestUniqueName);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    tagCount = rs.getInt(1);
                }
            }
            assertThat(tagCount)
                .as("V4 must dedup the tag table")
                .isEqualTo(1);

            int articleTagCount;
            try (ResultSet rs = st.executeQuery(
                    "SELECT count(*) FROM article_tag")) {
                rs.next();
                articleTagCount = rs.getInt(1);
            }
            assertThat(articleTagCount)
                .as("V4 must keep the article_tag row (remapped, not deleted)")
                .isEqualTo(1);

            int constraintCount;
            try (ResultSet rs = st.executeQuery(
                    "SELECT count(*) FROM pg_constraint WHERE conname = 'uc_tag_workspace_name_en'")) {
                rs.next();
                constraintCount = rs.getInt(1);
            }
            assertThat(constraintCount)
                .as("V4 must add the unique constraint")
                .isEqualTo(1);
        }
    }

    @Test
    @Order(5)
    void v4CategoryUniqueConstraintTreatsNullParentAsDistinct() throws Exception {
        // V4 must use NULLS NOT DISTINCT for the category unique constraint
        // (PostgreSQL 15+), so two root categories (parent_id IS NULL) with
        // the same name_en in the same workspace cannot coexist.

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = conn.createStatement()) {

            UUID wsId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            st.execute(String.format(
                "INSERT INTO app_user (id, email, password, display_name, role, enabled, created_at, updated_at) "
              + "VALUES ('%s', 'c@b', 'x', 'Admin', 'ADMIN', true, now(), now())", adminId));
            st.execute(String.format(
                "INSERT INTO workspace (id, name, slug, created_by, created_at, updated_at) "
              + "VALUES ('%s', 'ws-cat', 'ws-cat-%s', '%s', now(), now())", wsId, wsId, adminId));

            // Two root categories with the same name_en — the second INSERT
            // must fail because of the NULLS NOT DISTINCT constraint.
            st.execute(String.format(
                "INSERT INTO category (id, name_en, name_fr, workspace_id, created_at, updated_at) "
              + "VALUES ('%s', 'root-dup', 'root-dup', '%s', now(), now())",
                UUID.randomUUID(), wsId));

            boolean insertSucceeded = true;
            try {
                st.execute(String.format(
                    "INSERT INTO category (id, name_en, name_fr, workspace_id, created_at, updated_at) "
                  + "VALUES ('%s', 'root-dup', 'root-dup', '%s', now(), now())",
                    UUID.randomUUID(), wsId));
            } catch (Exception expected) {
                insertSucceeded = false;
            }
            assertThat(insertSucceeded)
                .as("V4's NULLS NOT DISTINCT constraint must reject duplicate root categories")
                .isFalse();
        }
    }
}
