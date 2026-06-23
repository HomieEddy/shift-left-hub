package com.shiftleft.hub.ai.service;

/**
 * Constants for AI configuration defaults.
 *
 * <p>Centralized so that the bootstrap default, validation floor, and any
 * future migrations (e.g. config import) agree on the same values.
 */
public final class AiDefaults {

    /** Default LLM provider for a fresh install. */
    public static final String LLM_PROVIDER = "OLLAMA";

    /** Default chat model. */
    public static final String CHAT_MODEL = "llama3.2:3b";

    /** Default embedding model. */
    public static final String EMBEDDING_MODEL = "nomic-embed-text";

    /** Default endpoint URL (Docker host on macOS/Windows). */
    public static final String OLLAMA_ENDPOINT = "http://host.docker.internal:11434";

    /** Default similarity threshold for vector retrieval. */
    public static final double SIMILARITY_THRESHOLD = 0.35;

    /** Minimum allowed similarity threshold (validation floor). */
    public static final double MIN_SIMILARITY_THRESHOLD = 0.65;

    /** Default embedding vector dimension (nomic-embed-text). */
    public static final int EMBEDDING_DIMENSION = 768;

    private AiDefaults() {
    }
}
