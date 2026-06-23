package com.shiftleft.hub.ai.api.dto;

import com.shiftleft.hub.ai.service.AiDefaults;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating or updating AI configuration.
 *
 * <p>The {@code similarityThreshold} floor of 0.65 enforces retrieval
 * quality on user-supplied values. The bootstrap default
 * ({@link com.shiftleft.hub.ai.service.AiDefaults#SIMILARITY_THRESHOLD})
 * is intentionally lower (0.35) because the dev environment is
 * pre-populated with curated seed documents.
 *
 * @param llmProvider         the LLM provider (OLLAMA or OPENAI)
 * @param ollamaEndpointUrl   the Ollama endpoint URL
 * @param openaiApiKey        the OpenAI API key
 * @param chatModelName       the chat model name
 * @param embeddingModelName  the embedding model name
 * @param similarityThreshold the similarity threshold for vector search
 */
public record AiConfigRequest(
    @Pattern(regexp = "^(OLLAMA|OPENAI|OPENAI_COMPATIBLE)$",
             message = "Provider must be OLLAMA, OPENAI, or OPENAI_COMPATIBLE")
    String llmProvider,

    @Pattern(regexp = "^https?://.*", message = "Must be a valid HTTP URL")
    String ollamaEndpointUrl,

    String openaiApiKey,

    @NotBlank(message = "Model name must not be blank")
    String chatModelName,

    String embeddingModelName,

    @DecimalMin(value = "0.65", inclusive = false) @DecimalMax("1.0")
    Double similarityThreshold
) {
}
