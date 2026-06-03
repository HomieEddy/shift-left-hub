package com.shiftleft.hub.ai.api.dto;

public record AiConfigRequest(
    String llmProvider,
    String ollamaEndpointUrl,
    String openaiApiKey,
    String chatModelName,
    String embeddingModelName,
    Double similarityThreshold
) {}
