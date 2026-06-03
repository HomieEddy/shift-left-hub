package com.shiftleft.hub.ai.api.dto;

import com.shiftleft.hub.ai.domain.AiConfig;

public record AiConfigResponse(
    String llmProvider,
    String ollamaEndpointUrl,
    boolean hasOpenaiKey,
    String chatModelName,
    String embeddingModelName,
    double similarityThreshold,
    int embeddingDimension
) {
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
