package com.shiftleft.hub.llmconfig.domain;

/**
 * Supported LLM provider types for per-workspace LLM configuration.
 * OLLAMA for local inference, OPENAI_COMPATIBLE for any OpenAI-compatible API endpoint.
 */
public enum LlmProvider {
    OLLAMA,
    OPENAI_COMPATIBLE
}
