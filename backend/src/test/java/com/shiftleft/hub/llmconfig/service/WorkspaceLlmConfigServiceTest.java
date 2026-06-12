package com.shiftleft.hub.llmconfig.service;

import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.llmconfig.api.dto.WorkspaceLlmConfigRequest;
import com.shiftleft.hub.llmconfig.domain.LlmProvider;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfig;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceLlmConfigServiceTest {

    @Mock private WorkspaceLlmConfigRepository repository;
    @Mock private AiConfigService aiConfigService;
    @Mock private WorkspaceChatModelRegistry chatModelRegistry;

    @InjectMocks private WorkspaceLlmConfigService workspaceLlmConfigService;

    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID CONFIG_ID = UUID.randomUUID();

    private WorkspaceLlmConfig createConfig() {
        WorkspaceLlmConfig config = WorkspaceLlmConfig.builder()
            .id(CONFIG_ID)
            .llmProvider(LlmProvider.OLLAMA)
            .endpointUrl("http://localhost:11434")
            .apiKey(null)
            .modelName("llama3.2")
            .embeddingModelName("nomic-embed-text")
            .similarityThreshold(0.65)
            .embeddingDimension(768)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        config.setWorkspaceId(WORKSPACE_ID);
        return config;
    }

    @Test
    void getConfig_shouldReturnConfigWhenExists() {
        when(repository.findByWorkspaceId(WORKSPACE_ID))
            .thenReturn(Optional.of(createConfig()));

        WorkspaceLlmConfig config = workspaceLlmConfigService.getConfig(WORKSPACE_ID);

        assertNotNull(config);
        assertEquals(WORKSPACE_ID, config.getWorkspaceId());
        assertEquals(LlmProvider.OLLAMA, config.getLlmProvider());
    }

    @Test
    void getConfig_shouldReturnNullWhenNotExists() {
        when(repository.findByWorkspaceId(WORKSPACE_ID))
            .thenReturn(Optional.empty());

        WorkspaceLlmConfig config = workspaceLlmConfigService.getConfig(WORKSPACE_ID);

        assertNull(config);
    }

    @Test
    void saveConfig_shouldCreateNewConfig() {
        when(repository.findByWorkspaceId(WORKSPACE_ID))
            .thenReturn(Optional.empty());
        when(repository.save(any(WorkspaceLlmConfig.class)))
            .thenReturn(createConfig());

        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, LlmProvider.OPENAI_COMPATIBLE, "https://api.openai.com/v1",
            "sk-test", "gpt-4", "text-embedding-3-small", 0.7);
        when(aiConfigService.encrypt("sk-test")).thenReturn("encrypted-key");

        WorkspaceLlmConfig result = workspaceLlmConfigService.saveConfig(WORKSPACE_ID, request);

        assertNotNull(result);
        verify(chatModelRegistry).evict(WORKSPACE_ID);
    }

    @Test
    void saveConfig_shouldUpdateExistingConfig() {
        WorkspaceLlmConfig existing = createConfig();
        when(repository.findByWorkspaceId(WORKSPACE_ID))
            .thenReturn(Optional.of(existing));
        when(repository.save(any(WorkspaceLlmConfig.class)))
            .thenReturn(existing);

        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, LlmProvider.OPENAI_COMPATIBLE, "https://custom-endpoint.com",
            null, "gpt-4o", null, 0.8);

        WorkspaceLlmConfig result = workspaceLlmConfigService.saveConfig(WORKSPACE_ID, request);

        assertEquals(LlmProvider.OPENAI_COMPATIBLE, result.getLlmProvider());
        assertEquals("https://custom-endpoint.com", result.getEndpointUrl());
        assertEquals("gpt-4o", result.getModelName());
        assertEquals(0.8, result.getSimilarityThreshold());
        verify(chatModelRegistry).evict(WORKSPACE_ID);
    }

    @Test
    void deleteConfig_shouldRemoveAndEvictCache() {
        doNothing().when(repository).deleteByWorkspaceId(WORKSPACE_ID);

        workspaceLlmConfigService.deleteConfig(WORKSPACE_ID);

        verify(repository).deleteByWorkspaceId(WORKSPACE_ID);
        verify(chatModelRegistry).evict(WORKSPACE_ID);
    }

    @Test
    void testConnection_shouldDelegateToAiConfigService() {
        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, LlmProvider.OLLAMA, "http://localhost:11434",
            null, "llama3.2", null, null);

        workspaceLlmConfigService.testConnection(WORKSPACE_ID, request);

        verify(aiConfigService).testConnection(
            "OLLAMA", "http://localhost:11434", null, "llama3.2");
    }

    // ── saveConfig: partial update ────────────────────────────────

    @Test
    void saveConfig_shouldUpdateOnlyNonNullFields() {
        WorkspaceLlmConfig existing = createConfig();
        existing.setApiKey("existing-encrypted-key");
        when(repository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(WorkspaceLlmConfig.class))).thenReturn(existing);

        // Only provide endpointUrl and similarityThreshold; leave rest null
        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, null, "https://new-endpoint.com", null, null, null, 0.8);

        WorkspaceLlmConfig result = workspaceLlmConfigService.saveConfig(WORKSPACE_ID, request);

        // Verify updated fields
        assertEquals("https://new-endpoint.com", result.getEndpointUrl());
        assertEquals(0.8, result.getSimilarityThreshold());
        // Verify untouched fields preserved existing values
        assertEquals(LlmProvider.OLLAMA, result.getLlmProvider());
        assertEquals("existing-encrypted-key", result.getApiKey());
        assertEquals("llama3.2", result.getModelName());
        assertEquals("nomic-embed-text", result.getEmbeddingModelName());
        verify(chatModelRegistry).evict(WORKSPACE_ID);
    }

    // ── saveConfig: encryption guard ─────────────────────────────

    @Test
    void saveConfig_shouldNotEncryptNullApiKey() {
        WorkspaceLlmConfig existing = createConfig();
        when(repository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(WorkspaceLlmConfig.class))).thenReturn(existing);

        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, null, null, null, null, null, null);

        workspaceLlmConfigService.saveConfig(WORKSPACE_ID, request);

        verify(aiConfigService, never()).encrypt(anyString());
        verify(chatModelRegistry).evict(WORKSPACE_ID);
    }

    @Test
    void saveConfig_shouldNotEncryptBlankApiKey() {
        WorkspaceLlmConfig existing = createConfig();
        when(repository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(WorkspaceLlmConfig.class))).thenReturn(existing);

        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, null, null, "", null, null, null);

        workspaceLlmConfigService.saveConfig(WORKSPACE_ID, request);

        verify(aiConfigService, never()).encrypt(anyString());
        verify(chatModelRegistry).evict(WORKSPACE_ID);
    }

    // ── testConnection: API key fallback ─────────────────────────

    @Test
    void testConnection_shouldFallbackToExistingEncryptedKey() {
        WorkspaceLlmConfig existing = createConfig();
        existing.setApiKey("stored-encrypted-key");
        when(repository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(existing));

        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, LlmProvider.OLLAMA, "http://localhost:11434",
            null, "llama3.2", null, null);

        workspaceLlmConfigService.testConnection(WORKSPACE_ID, request);

        // Should fall back to the existing encrypted key
        verify(aiConfigService).testConnection(
            "OLLAMA", "http://localhost:11434", "stored-encrypted-key", "llama3.2");
    }

    @Test
    void testConnection_shouldNotFallbackWhenNoExistingConfig() {
        when(repository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());

        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, LlmProvider.OLLAMA, "http://localhost:11434",
            null, "llama3.2", null, null);

        workspaceLlmConfigService.testConnection(WORKSPACE_ID, request);

        // No existing config → apiKey stays null
        verify(aiConfigService).testConnection(
            "OLLAMA", "http://localhost:11434", null, "llama3.2");
    }

    @Test
    void testConnection_shouldUseApiKeyFromRequestWhenProvided() {
        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            WORKSPACE_ID, LlmProvider.OLLAMA, "http://localhost:11434",
            "sk-explicit-key", "llama3.2", null, null);

        workspaceLlmConfigService.testConnection(WORKSPACE_ID, request);

        // apiKey is non-null in request → no config lookup needed
        verify(repository, never()).findByWorkspaceId(any());
        verify(aiConfigService).testConnection(
            "OLLAMA", "http://localhost:11434", "sk-explicit-key", "llama3.2");
    }

    @Test
    void testConnection_shouldSkipConfigLookupWhenWorkspaceIdIsNull() {
        WorkspaceLlmConfigRequest request = new WorkspaceLlmConfigRequest(
            null, LlmProvider.OLLAMA, "http://localhost:11434",
            null, "llama3.2", null, null);

        workspaceLlmConfigService.testConnection(null, request);

        // workspaceId is null → short-circuits before config lookup
        verify(repository, never()).findByWorkspaceId(any());
        verify(aiConfigService).testConnection(
            "OLLAMA", "http://localhost:11434", null, "llama3.2");
    }
}
