package com.shiftleft.hub.llmconfig.service;

import com.shiftleft.hub.ai.api.dto.TestConnectionResult;
import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.llmconfig.api.dto.WorkspaceLlmConfigRequest;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfig;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing per-workspace LLM configuration.
 * Provides CRUD operations and connection testing for workspace-scoped LLM settings.
 * Falls back to global AiConfig defaults when no workspace config exists (D-18).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WorkspaceLlmConfigService {

    private final WorkspaceLlmConfigRepository repository;
    private final AiConfigService aiConfigService;
    private final WorkspaceChatModelRegistry chatModelRegistry;

    /**
     * Returns the LLM configuration for a workspace, or null if not configured.
     *
     * @param workspaceId the workspace UUID
     * @return the workspace LLM config, or null
     */
    public WorkspaceLlmConfig getConfig(UUID workspaceId) {
        return repository.findByWorkspaceId(workspaceId).orElse(null);
    }

    /**
     * Saves or updates LLM configuration for a workspace.
     * API key is encrypted at rest via AiConfigService.encrypt() (D-20).
     *
     * @param workspaceId the workspace UUID
     * @param request     the configuration to save
     * @return the saved workspace LLM config
     */
    @Transactional
    public WorkspaceLlmConfig saveConfig(UUID workspaceId, WorkspaceLlmConfigRequest request) {
        WorkspaceLlmConfig config = repository.findByWorkspaceId(workspaceId)
            .orElseGet(() -> {
                WorkspaceLlmConfig c = WorkspaceLlmConfig.builder().build();
                c.setWorkspaceId(workspaceId);
                return c;
            });

        if (request.llmProvider() != null) {
            config.setLlmProvider(request.llmProvider());
        }
        if (request.endpointUrl() != null) {
            config.setEndpointUrl(request.endpointUrl());
        }
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            config.setApiKey(aiConfigService.encrypt(request.apiKey()));
        }
        if (request.modelName() != null) {
            config.setModelName(request.modelName());
        }
        if (request.embeddingModelName() != null) {
            config.setEmbeddingModelName(request.embeddingModelName());
        }
        if (request.similarityThreshold() != null) {
            config.setSimilarityThreshold(request.similarityThreshold());
        }

        config.setSystemPrompt(request.systemPrompt());

        config = repository.save(config);
        chatModelRegistry.evict(workspaceId);
        log.info("LLM config saved for workspace {} (provider: {})", workspaceId, config.getLlmProvider());
        return config;
    }

    /**
     * Deletes the LLM configuration for a workspace.
     *
     * @param workspaceId the workspace UUID
     */
    @Transactional
    public void deleteConfig(UUID workspaceId) {
        repository.deleteByWorkspaceId(workspaceId);
        chatModelRegistry.evict(workspaceId);
        log.info("LLM config deleted for workspace {}", workspaceId);
    }

    /**
     * Tests a connection to the LLM provider with the given configuration.
     * If the API key is not provided in the request but a config exists, uses the existing encrypted key.
     *
     * @param workspaceId the workspace UUID (used for fallback to stored API key)
     * @param request     the configuration to test
     * @return connection test result
     */
    public TestConnectionResult testConnection(UUID workspaceId, WorkspaceLlmConfigRequest request) {
        String apiKey = request.apiKey();
        // If no apiKey in request but config exists, use existing encrypted key
        if ((apiKey == null || apiKey.isBlank()) && workspaceId != null) {
            WorkspaceLlmConfig existing = getConfig(workspaceId);
            if (existing != null) {
                apiKey = existing.getApiKey();
            }
        }
        return aiConfigService.testConnection(
            request.llmProvider() != null ? request.llmProvider().name() : "OLLAMA",
            request.endpointUrl(),
            apiKey,
            request.modelName() != null ? request.modelName() : "llama3.2"
        );
    }
}
