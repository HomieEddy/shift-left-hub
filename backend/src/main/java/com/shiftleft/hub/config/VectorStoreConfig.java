package com.shiftleft.hub.config;

import com.shiftleft.hub.ai.service.EmbeddingModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Provides a PgVectorStore bean that uses the dynamic EmbeddingModelProvider
 * instead of the auto-configured Ollama singleton.
 */
@Configuration
@Slf4j
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
    private String schemaName;

    /**
     * Creates a PgVectorStore backed by the dynamic EmbeddingModelProvider.
     * Overrides the Spring AI auto-configured bean.
     *
     * @param jdbcTemplate       the JDBC template
     * @param embeddingProvider  the dynamic embedding model provider
     * @return a configured PgVectorStore
     */
    @Bean
    public PgVectorStore vectorStore(JdbcTemplate jdbcTemplate,
            EmbeddingModelProvider embeddingProvider) {
        logVectorStoreSchema(jdbcTemplate);
        EmbeddingModel embeddingModel = embeddingProvider.getEmbeddingModel();
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .schemaName(schemaName)
            .dimensions(dimensions)
            .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .build();
    }

    private void logVectorStoreSchema(JdbcTemplate jdbcTemplate) {
        try {
            String embeddingType = jdbcTemplate.queryForObject("""
                SELECT format_type(a.atttypid, a.atttypmod)
                FROM pg_attribute a
                JOIN pg_class c ON c.oid = a.attrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ?
                  AND c.relname = 'vector_store'
                  AND a.attname = 'embedding'
                  AND NOT a.attisdropped
                """, String.class, schemaName);
            Boolean vectorExtensionInstalled = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_extension WHERE extname = 'vector'
                )
                """, Boolean.class);
            log.info(
                "PgVectorStore configured: schema={}, dimensions={}, embeddingColumnType={}, vectorExt={}",
                schemaName, dimensions, embeddingType, vectorExtensionInstalled);
        } catch (Exception e) {
            log.warn("Unable to inspect PgVectorStore schema: {}", mostSpecificMessage(e));
        }
    }

    private String mostSpecificMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
