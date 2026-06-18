package com.shiftleft.hub.config;

import com.shiftleft.hub.ai.service.EmbeddingModelProvider;
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
        EmbeddingModel embeddingModel = embeddingProvider.getEmbeddingModel();
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .schemaName(schemaName)
            .dimensions(dimensions)
            .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .build();
    }
}
