package com.shiftleft.hub.ai.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    @DecimalMin(value = "0.65", inclusive = false) @DecimalMax("1.0")
    Double similarityThreshold
) {}
