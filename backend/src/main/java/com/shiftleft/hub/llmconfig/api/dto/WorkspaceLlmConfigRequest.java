package com.shiftleft.hub.llmconfig.api.dto;

import com.shiftleft.hub.llmconfig.domain.LlmProvider;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Request DTO for creating or updating a workspace LLM configuration.
 *
 * @param workspaceId         the workspace UUID (used for fallback in connection test)
 * @param llmProvider         the LLM provider (OLLAMA or OPENAI_COMPATIBLE)
 * @param endpointUrl         the endpoint URL for the LLM provider
 * @param apiKey              the API key (encrypted at rest via AiConfigService)
 * @param modelName           the chat model name
 * @param embeddingModelName  the embedding model name
 * @param similarityThreshold the similarity threshold for vector search (0.65 exclusive to 1.0)
 */
public record WorkspaceLlmConfigRequest(
    UUID workspaceId,
    LlmProvider llmProvider,

    @Pattern(regexp = "^https?://.*", message = "Must be a valid HTTP URL")
    String endpointUrl,

    String apiKey,
    String modelName,
    String embeddingModelName,

    @DecimalMin(value = "0.65", inclusive = false) @DecimalMax("1.0")
    Double similarityThreshold,

    String systemPrompt
) {
}
