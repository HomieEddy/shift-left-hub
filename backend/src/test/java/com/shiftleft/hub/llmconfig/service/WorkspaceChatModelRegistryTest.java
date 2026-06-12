package com.shiftleft.hub.llmconfig.service;

import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.llmconfig.domain.LlmProvider;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfig;
import com.shiftleft.hub.llmconfig.domain.WorkspaceLlmConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceChatModelRegistryTest {

    @Mock private WorkspaceLlmConfigRepository workspaceLlmConfigRepository;
    @Mock private AiConfigService aiConfigService;
    @Mock private ChatClient mockChatClient;

    @InjectMocks private WorkspaceChatModelRegistry registry;

    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID OTHER_WORKSPACE_ID = UUID.randomUUID();

    private WorkspaceLlmConfig createWorkspaceConfig() {
        WorkspaceLlmConfig config = WorkspaceLlmConfig.builder()
            .id(UUID.randomUUID())
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
    void getChatClient_shouldBuildFromWorkspaceConfig() {
        WorkspaceLlmConfig wsConfig = createWorkspaceConfig();
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(wsConfig));
        when(aiConfigService.buildChatClient(
            eq("OLLAMA"), eq("http://localhost:11434"), isNull(), eq("llama3.2")))
            .thenReturn(mockChatClient);

        ChatClient client = registry.getChatClient(WORKSPACE_ID);

        assertNotNull(client);
        assertEquals(mockChatClient, client);
    }

    @Test
    void getChatClient_shouldFallbackToGlobalConfig() {
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());
        var globalAiConfig = new com.shiftleft.hub.ai.domain.AiConfig();
        when(aiConfigService.getConfigEntity()).thenReturn(globalAiConfig);
        when(aiConfigService.buildChatClient(globalAiConfig)).thenReturn(mockChatClient);

        ChatClient client = registry.getChatClient(WORKSPACE_ID);

        assertNotNull(client);
        assertEquals(mockChatClient, client);
    }

    @Test
    void getChatClient_shouldCacheResults() {
        WorkspaceLlmConfig wsConfig = createWorkspaceConfig();
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(wsConfig));
        when(aiConfigService.buildChatClient(
            anyString(), anyString(), any(), anyString()))
            .thenReturn(mockChatClient);

        ChatClient first = registry.getChatClient(WORKSPACE_ID);
        ChatClient second = registry.getChatClient(WORKSPACE_ID);

        assertSame(first, second);
        verify(aiConfigService, times(1)).buildChatClient(anyString(), anyString(), any(), anyString());
    }

    @Test
    void evict_shouldRemoveCacheEntry() {
        WorkspaceLlmConfig wsConfig = createWorkspaceConfig();
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(wsConfig));
        when(aiConfigService.buildChatClient(
            anyString(), anyString(), any(), anyString()))
            .thenReturn(mockChatClient);

        registry.getChatClient(WORKSPACE_ID);
        registry.evict(WORKSPACE_ID);

        when(aiConfigService.buildChatClient(
            anyString(), anyString(), any(), anyString()))
            .thenReturn(mock(ChatClient.class));

        ChatClient afterEvict = registry.getChatClient(WORKSPACE_ID);
        assertNotNull(afterEvict);
        verify(aiConfigService, times(2)).buildChatClient(anyString(), anyString(), any(), anyString());
    }

    @Test
    void evictAll_shouldClearEntireCache() {
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(workspaceLlmConfigRepository.findByWorkspaceId(OTHER_WORKSPACE_ID)).thenReturn(Optional.empty());
        var globalAiConfig = new com.shiftleft.hub.ai.domain.AiConfig();
        when(aiConfigService.getConfigEntity()).thenReturn(globalAiConfig);
        when(aiConfigService.buildChatClient(globalAiConfig)).thenReturn(mockChatClient);

        registry.getChatClient(WORKSPACE_ID);
        registry.getChatClient(OTHER_WORKSPACE_ID);
        registry.evictAll();

        when(aiConfigService.buildChatClient(globalAiConfig)).thenReturn(mock(ChatClient.class));
        registry.getChatClient(WORKSPACE_ID);
        verify(aiConfigService, times(3)).buildChatClient(globalAiConfig);
    }

    @Test
    void getWorkspaceConfig_shouldReturnConfig() {
        WorkspaceLlmConfig wsConfig = createWorkspaceConfig();
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(wsConfig));

        WorkspaceLlmConfig result = registry.getWorkspaceConfig(WORKSPACE_ID);

        assertNotNull(result);
        assertEquals(WORKSPACE_ID, result.getWorkspaceId());
    }

    @Test
    void getWorkspaceConfig_shouldReturnNullWhenNotExists() {
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());

        WorkspaceLlmConfig result = registry.getWorkspaceConfig(WORKSPACE_ID);

        assertNull(result);
    }

    // ── OPENAI_COMPATIBLE provider ────────────────────────────────

    @Test
    void getChatClient_shouldBuildWithOpenaiCompatibleProvider() {
        WorkspaceLlmConfig wsConfig = createWorkspaceConfig();
        wsConfig.setLlmProvider(LlmProvider.OPENAI_COMPATIBLE);
        wsConfig.setApiKey("encrypted-openai-key");
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(wsConfig));
        when(aiConfigService.buildChatClient(
            eq("OPENAI_COMPATIBLE"), eq("http://localhost:11434"), eq("encrypted-openai-key"), eq("llama3.2")))
            .thenReturn(mockChatClient);

        ChatClient client = registry.getChatClient(WORKSPACE_ID);

        assertNotNull(client);
        assertEquals(mockChatClient, client);
        verify(aiConfigService).buildChatClient(
            "OPENAI_COMPATIBLE", "http://localhost:11434", "encrypted-openai-key", "llama3.2");
    }

    // ── Sequential evict + recreate ──────────────────────────────

    @Test
    void evictAndRecreate_shouldWorkSequentially() {
        WorkspaceLlmConfig wsConfig = createWorkspaceConfig();
        when(workspaceLlmConfigRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(wsConfig));

        ChatClient client1 = mock(ChatClient.class);
        ChatClient client2 = mock(ChatClient.class);
        ChatClient client3 = mock(ChatClient.class);
        when(aiConfigService.buildChatClient(anyString(), anyString(), any(), anyString()))
            .thenReturn(client1, client2, client3);

        // First call — creates
        assertNotNull(registry.getChatClient(WORKSPACE_ID));
        // Second call — cached, no new create
        assertNotNull(registry.getChatClient(WORKSPACE_ID));

        registry.evict(WORKSPACE_ID);

        // After evict — creates new
        assertNotNull(registry.getChatClient(WORKSPACE_ID));

        registry.evict(WORKSPACE_ID);

        // After second evict — creates new again
        assertNotNull(registry.getChatClient(WORKSPACE_ID));

        verify(aiConfigService, times(3))
            .buildChatClient(anyString(), anyString(), any(), anyString());
    }

    // ── init() ───────────────────────────────────────────────────

    @Test
    void init_shouldNotThrow() {
        assertDoesNotThrow(() -> registry.init());
    }
}
