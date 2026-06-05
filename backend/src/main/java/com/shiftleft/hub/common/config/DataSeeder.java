package com.shiftleft.hub.common.config;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
@Order(1)
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final AiConfigRepository aiConfigRepository;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminPassword == null) {
            log.info("Admin seeder skipped — set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD to seed admin user");
        } else {
            seedUser(adminEmail, "System Admin", UserRole.ROLE_ADMIN);
            seedUser(deriveEmail("user"), "Regular User", UserRole.ROLE_USER);
            seedUser(deriveEmail("tech"), "Tech Agent", UserRole.ROLE_AGENT);
        }
        setupFullTextSearch();
        setupVectorSearch();
        seedAiConfig();
    }

    private void seedUser(String email, String displayName, UserRole role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(adminPassword))
                .displayName(displayName)
                .role(role)
                .enabled(true)
                .build()
            );
            log.info("Created {} user with email: {}", role, email);
        } else {
            log.info("{} user {} already exists — skipping", role, email);
        }
    }

    private String deriveEmail(String prefix) {
        int at = adminEmail.indexOf('@');
        if (at == -1) return prefix + "@shiftleft.com";
        return prefix + adminEmail.substring(at);
    }

    private void setupFullTextSearch() {
        log.info("Setting up full-text search...");

        jdbcTemplate.execute("""
            ALTER TABLE article
            ADD COLUMN IF NOT EXISTS tsv_en TSVECTOR
        """);

        jdbcTemplate.execute("""
            ALTER TABLE article
            ADD COLUMN IF NOT EXISTS tsv_fr TSVECTOR
        """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_article_tsv_en
            ON article USING GIN (tsv_en)
        """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_article_tsv_fr
            ON article USING GIN (tsv_fr)
        """);

        jdbcTemplate.execute("""
            CREATE OR REPLACE FUNCTION update_article_tsv()
            RETURNS TRIGGER AS $$
            BEGIN
                NEW.tsv_en := to_tsvector('english', COALESCE(NEW.title_en, '') || ' ' || COALESCE(NEW.content_en, ''));
                NEW.tsv_fr := to_tsvector('french', COALESCE(NEW.title_fr, '') || ' ' || COALESCE(NEW.content_fr, ''));
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
        """);

        jdbcTemplate.execute("""
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1 FROM pg_trigger
                    WHERE tgname = 'trigger_article_tsv'
                ) THEN
                    CREATE TRIGGER trigger_article_tsv
                    BEFORE INSERT OR UPDATE OF title_en, content_en, title_fr, content_fr
                    ON article
                    FOR EACH ROW
                    EXECUTE FUNCTION update_article_tsv();
                END IF;
            END;
            $$;
        """);

        jdbcTemplate.execute("""
            UPDATE article
            SET
              tsv_en = to_tsvector('english', COALESCE(title_en, '') || ' ' || COALESCE(content_en, '')),
              tsv_fr = to_tsvector('french', COALESCE(title_fr, '') || ' ' || COALESCE(content_fr, ''))
            WHERE tsv_en IS NULL OR tsv_fr IS NULL
        """);

        log.info("Full-text search setup complete.");
    }

    private void setupVectorSearch() {
        log.info("Setting up vector search...");
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vector_store (
                    id UUID PRIMARY KEY,
                    content TEXT,
                    metadata JSONB,
                    embedding vector(768)
                )
            """);
            log.info("Vector search setup complete.");
        } catch (Exception e) {
            log.warn("Vector extension is not available on this PostgreSQL instance. Skipping vector setup.");
        }
    }

    private void seedAiConfig() {
        if (aiConfigRepository.count() == 0) {
            AiConfig config = AiConfig.builder()
                .llmProvider("OLLAMA")
                .ollamaEndpointUrl("http://host.docker.internal:11434")
                .openaiApiKey(null)
                .chatModelName("llama3.2:3b")
                .embeddingModelName("nomic-embed-text")
                .similarityThreshold(0.7)
                .embeddingDimension(768)
                .build();
            aiConfigRepository.save(config);
            log.info("Created default AI config (Ollama local)");
        }
    }
}
