package com.shiftleft.hub.llmconfig.service;

import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfig;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of cached ChatClient instances per workspace.
 * <p>
 * Provides workspace-scoped ChatClients with lazy creation and caching.
 * Falls back to global AiConfig defaults when no workspace LLM config exists (D-18).
 * Cache is evicted on workspace config update or delete.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceChatModelRegistry {

    private final WorkspaceLlmConfigRepository workspaceLlmConfigRepository;
    private final AiConfigService aiConfigService;

    private final Map<UUID, ChatClient> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("WorkspaceChatModelRegistry initialized");
    }

    /**
     * Returns a ChatClient for the given workspace.
     * Uses workspace LLM config if available, falls back to global AiConfig defaults.
     * Results are cached — call {@link #evict(UUID)} to clear cache after config update.
     *
     * @param workspaceId the workspace UUID
     * @return a configured ChatClient
     */
    public ChatClient getChatClient(UUID workspaceId) {
        return cache.computeIfAbsent(workspaceId, this::buildChatClient);
    }

    /**
     * Returns the workspace LLM config if available.
     * Used by AiChatService to read threshold and other non-ChatModel settings.
     *
     * @param workspaceId the workspace UUID
     * @return the workspace LLM config, or null
     */
    public WorkspaceLlmConfig getWorkspaceConfig(UUID workspaceId) {
        return workspaceLlmConfigRepository.findByWorkspaceId(workspaceId).orElse(null);
    }

    /**
     * Evicts the cached ChatClient for a workspace.
     * Called after workspace LLM config is updated.
     *
     * @param workspaceId the workspace UUID
     */
    public void evict(UUID workspaceId) {
        cache.remove(workspaceId);
        log.debug("Evicted ChatClient cache for workspace {}", workspaceId);
    }

    /**
     * Evicts all cached ChatClients.
     */
    public void evictAll() {
        cache.clear();
        log.debug("Evicted all ChatClient caches");
    }

    /**
     * Builds a ChatClient for a workspace, checking workspace config first
     * and falling back to global AiConfig defaults.
     */
    private ChatClient buildChatClient(UUID workspaceId) {
        WorkspaceLlmConfig workspaceConfig = workspaceLlmConfigRepository.findByWorkspaceId(workspaceId).orElse(null);

        if (workspaceConfig != null) {
            log.debug("Building workspace-scoped ChatClient for workspace {} (provider: {})",
                workspaceId, workspaceConfig.getLlmProvider());
            return aiConfigService.buildChatClient(
                workspaceConfig.getLlmProvider().name(),
                workspaceConfig.getEndpointUrl(),
                workspaceConfig.getApiKey(),
                workspaceConfig.getModelName()
            );
        }

        // Fall back to global defaults
        log.debug("No workspace LLM config for {}, using global defaults", workspaceId);
        var globalConfig = aiConfigService.getConfigEntity();
        return aiConfigService.buildChatClient(globalConfig);
    }
}
