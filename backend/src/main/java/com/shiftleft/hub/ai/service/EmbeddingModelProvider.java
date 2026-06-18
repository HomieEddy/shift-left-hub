package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.config.EmbeddingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * Provides a lazily-cached EmbeddingModel built from environment-variable-based
 * EmbeddingProperties. The cache is invalidated when the admin saves AI config
 * so the model rebuilds with current settings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingModelProvider {

    private final AiConfigService aiConfigService;
    private final EmbeddingProperties embeddingProperties;

    private volatile EmbeddingModel cached;

    public EmbeddingModel getEmbeddingModel() {
        if (cached == null) {
            synchronized (this) {
                if (cached == null) {
                    log.info("Building EmbeddingModel from EmbeddingProperties");
                    cached = aiConfigService.buildEmbeddingModel(embeddingProperties);
                }
            }
        }
        return cached;
    }

    public void evict() {
        cached = null;
        log.info("EmbeddingModel cache evicted — will rebuild on next request");
    }
}
