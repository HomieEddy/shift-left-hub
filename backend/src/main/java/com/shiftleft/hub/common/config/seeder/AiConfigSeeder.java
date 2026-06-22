package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Seeds the default AI config (Ollama, llama3.2:3b chat, nomic-embed-text embeddings)
 * if no config exists.
 *
 * <p>Single responsibility: idempotent default AI config creation. Uses
 * a {@code host.docker.internal} endpoint so the same default works in
 * dev containers and on host machines.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiConfigSeeder {

    private final AiConfigRepository aiConfigRepository;

    /**
     * Creates the default AI config if none exists. Idempotent: skips
     * if {@code count() != 0}.
     */
    public void seedAiConfig() {
        if (aiConfigRepository.count() != 0) {
            log.debug("AI config already exists - skipping");
            return;
        }
        AiConfig config = AiConfig.builder()
            .llmProvider("OLLAMA")
            .ollamaEndpointUrl("http://host.docker.internal:11434")
            .openaiApiKey(null)
            .chatModelName("llama3.2:3b")
            .embeddingModelName("nomic-embed-text")
            .similarityThreshold(0.7)
            .embeddingDimension(768)
            .build();
        aiConfigRepository.save(config);
        log.info("Created default AI config (Ollama - {} / {})",
            config.getChatModelName(), config.getEmbeddingModelName());
    }
}
