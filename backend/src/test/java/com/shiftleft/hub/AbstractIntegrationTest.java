package com.shiftleft.hub;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests.
 * <p>Starts a pgvector PostgreSQL container via Testcontainers,
 * applies Flyway migrations, and provides a {@code @ServiceConnection}
 * so that Spring Boot auto-configures the datasource.
 * Extend this class for any test that needs a real database.</p>
 *
 * <p>Never use H2 for integration tests — PostgreSQL-specific features
 * (JSONB, tsvector, pgvector) are not supported by H2.</p>
 *
 * <p>Subclasses should annotate with {@code @SpringBootTest} and
 * {@code @ActiveProfiles("test")} if they need the test profile.</p>
 *
 * @see <a href="https://testcontainers.com/guides/testing-spring-boot-rest-api/">Testcontainers Spring Boot Guide</a>
 */
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.0-pg16")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
