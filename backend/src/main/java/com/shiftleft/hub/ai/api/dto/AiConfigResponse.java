package com.shiftleft.hub.ai.api.dto;

import com.shiftleft.hub.ai.domain.AiConfig;

/**
 * Response DTO for AI configuration.
 *
 * @param llmProvider        the LLM provider
 * @param ollamaEndpointUrl  the Ollama endpoint URL
 * @param hasOpenaiKey       whether an OpenAI API key is configured
 * @param chatModelName      the chat model name
 * @param embeddingModelName the embedding model name
 * @param similarityThreshold the similarity threshold for vector search
 * @param embeddingDimension the embedding vector dimension
 */
public record AiConfigResponse(
    String llmProvider,
    String ollamaEndpointUrl,
    boolean hasOpenaiKey,
    String chatModelName,
    String embeddingModelName,
    double similarityThreshold,
    int embeddingDimension
) {
    /**
     * Creates an AiConfigResponse from an AiConfig entity.
     *
     * @param config the AI configuration entity
     * @return a new AiConfigResponse
     */
    public static AiConfigResponse from(AiConfig config) {
        return new AiConfigResponse(
            config.getLlmProvider(),
            config.getOllamaEndpointUrl(),
            config.getOpenaiApiKey() != null && !config.getOpenaiApiKey().isEmpty(),
            config.getChatModelName(),
            config.getEmbeddingModelName(),
            config.getSimilarityThreshold(),
            config.getEmbeddingDimension()
        );
    }
}
