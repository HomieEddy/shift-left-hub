package com.shiftleft.hub.ai.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AiConfigRequest(
    @Pattern(regexp = "^(OLLAMA|OPENAI)$", message = "Provider must be OLLAMA or OPENAI")
    String llmProvider,

    @Pattern(regexp = "^https?://.*", message = "Must be a valid HTTP URL")
    String ollamaEndpointUrl,

    String openaiApiKey,

    @NotBlank(message = "Model name must not be blank")
    String chatModelName,

    String embeddingModelName,

    @Min(0) @Max(1)
    Double similarityThreshold
) {}
