package com.shiftleft.hub.llmconfig.api.dto;

import com.shiftleft.hub.llmconfig.domain.LlmProvider;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfig;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for workspace LLM configuration.
 * Excludes the API key for security (never returned in API responses).
 *
 * @param id                  the entity ID
 * @param workspaceId         the workspace UUID
 * @param llmProvider         the LLM provider
 * @param endpointUrl         the endpoint URL
 * @param modelName           the chat model name
 * @param embeddingModelName  the embedding model name
 * @param similarityThreshold the similarity threshold
 * @param embeddingDimension  the embedding vector dimension
 * @param createdAt           the creation timestamp
 * @param updatedAt           the last update timestamp
 */
public record WorkspaceLlmConfigResponse(
    UUID id,
    UUID workspaceId,
    LlmProvider llmProvider,
    String endpointUrl,
    String modelName,
    String embeddingModelName,
    Double similarityThreshold,
    Integer embeddingDimension,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Creates a WorkspaceLlmConfigResponse from a WorkspaceLlmConfig entity.
     * Returns null if the input is null.
     *
     * @param config the workspace LLM config entity
     * @return a new response DTO, or null
     */
    public static WorkspaceLlmConfigResponse from(WorkspaceLlmConfig config) {
        if (config == null) {
            return null;
        }
        return new WorkspaceLlmConfigResponse(
            config.getId(),
            config.getWorkspaceId(),
            config.getLlmProvider(),
            config.getEndpointUrl(),
            config.getModelName(),
            config.getEmbeddingModelName(),
            config.getSimilarityThreshold(),
            config.getEmbeddingDimension(),
            config.getCreatedAt(),
            config.getUpdatedAt()
        );
    }
}
