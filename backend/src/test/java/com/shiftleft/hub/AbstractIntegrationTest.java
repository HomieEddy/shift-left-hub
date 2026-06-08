package com.shiftleft.hub;

import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * <p>Uses the Testcontainers JDBC URL {@code jdbc:tc:pgvector:0.8.0-pg16:///testdb}
 * which automatically starts a pgvector PostgreSQL container. Flyway applies
 * schema migrations on startup.</p>
 *
 * <p>Never use H2 for integration tests — PostgreSQL-specific features
 * (JSONB, tsvector, pgvector) are not supported by H2.</p>
 *
 * <p>Subclasses should annotate with {@code @SpringBootTest} if they
 * need the full application context.</p>
 */
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    // Connection is configured via application-test.properties
    // using jdbc:tc:pgvector:0.8.0-pg16:///testdb (Testcontainers JDBC URL)
}
