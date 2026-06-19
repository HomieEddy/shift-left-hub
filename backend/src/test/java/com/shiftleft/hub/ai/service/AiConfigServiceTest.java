package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.ai.api.dto.AiConfigRequest;
import com.shiftleft.hub.ai.api.dto.AiConfigResponse;
import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import com.shiftleft.hub.config.EmbeddingProperties;
import com.shiftleft.hub.llmconfig.service.WorkspaceChatModelRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConfigServiceTest {

    @Mock private AiConfigRepository aiConfigRepository;
    @Mock private EmbeddingModelProvider embeddingProvider;
    @Mock private WorkspaceChatModelRegistry workspaceChatModelRegistry;

    @InjectMocks private AiConfigService aiConfigService;

    private AiConfig defaultConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = AiConfig.builder()
            .llmProvider("OLLAMA")
            .ollamaEndpointUrl("http://host.docker.internal:11434")
            .chatModelName("llama3.2:3b")
            .embeddingModelName("nomic-embed-text")
            .similarityThreshold(0.65)
            .embeddingDimension(768)
            .build();
        ReflectionTestUtils.setField(aiConfigService, "encryptionKey", "test-encryption-key-32chars!!");
        ReflectionTestUtils.setField(aiConfigService, "encryptionSalt", "test-salt");
    }

    // ── getConfig ────────────────────────────────────────────

    @Test
    void getConfig_shouldReturnAiConfigWhenExists() {
        when(aiConfigRepository.findSingleConfig()).thenReturn(Optional.of(defaultConfig));

        AiConfigResponse response = aiConfigService.getConfig();

        assertNotNull(response);
        assertEquals("OLLAMA", response.llmProvider());
        assertEquals("http://host.docker.internal:11434", response.ollamaEndpointUrl());
    }

    @Test
    void getConfig_shouldReturnDefaultWhenNotExists() {
        when(aiConfigRepository.findSingleConfig()).thenReturn(Optional.empty());
        when(aiConfigRepository.save(any(AiConfig.class))).thenReturn(defaultConfig);

        AiConfigResponse response = aiConfigService.getConfig();

        assertNotNull(response);
        assertEquals("OLLAMA", response.llmProvider());
        verify(aiConfigRepository).save(any(AiConfig.class));
    }

    // ── updateConfig ─────────────────────────────────────────

    @Test
    void updateConfig_shouldUpdateLlmProvider() {
        when(aiConfigRepository.findSingleConfig()).thenReturn(Optional.of(defaultConfig));
        when(aiConfigRepository.save(any(AiConfig.class))).thenReturn(defaultConfig);

        AiConfigRequest request = new AiConfigRequest(
            "OPENAI", null, null, "gpt-4", null, null);
        AiConfigResponse response = aiConfigService.updateConfig(request);

        assertNotNull(response);
        verify(aiConfigRepository).save(any(AiConfig.class));
    }

    @Test
    void updateConfig_shouldEncryptApiKey() {
        when(aiConfigRepository.findSingleConfig()).thenReturn(Optional.of(defaultConfig));
        when(aiConfigRepository.save(any(AiConfig.class))).thenReturn(defaultConfig);

        AiConfigRequest request = new AiConfigRequest(
            null, null, "sk-raw-key", null, null, null);
        aiConfigService.updateConfig(request);

        ArgumentCaptor<AiConfig> captor = ArgumentCaptor.forClass(AiConfig.class);
        verify(aiConfigRepository).save(captor.capture());
        AiConfig saved = captor.getValue();
        assertNotNull(saved.getOpenaiApiKey());
        assertNotEquals("sk-raw-key", saved.getOpenaiApiKey());
    }

    @Test
    void updateConfig_shouldNotEncryptBlankApiKey() {
        when(aiConfigRepository.findSingleConfig()).thenReturn(Optional.of(defaultConfig));
        when(aiConfigRepository.save(any(AiConfig.class))).thenReturn(defaultConfig);

        AiConfigRequest request = new AiConfigRequest(
            null, null, "", null, null, null);
        aiConfigService.updateConfig(request);

        verify(aiConfigRepository).save(any(AiConfig.class));
    }

    // ── encrypt / decrypt ─────────────────────────────────────

    @Test
    void encrypt_shouldReturnEncryptedString() {
        String plaintext = "sk-test-api-key-12345";

        String encrypted = aiConfigService.encrypt(plaintext);

        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
        assertNotEquals(plaintext, encrypted);
    }

    @Test
    void decrypt_shouldReturnOriginalValue() {
        String plaintext = "sk-test-api-key-12345";
        String encrypted = aiConfigService.encrypt(plaintext);

        String decrypted = aiConfigService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_decrypt_shouldBeConsistent() {
        String plaintext = "another-test-key-with-special-chars!@#$";

        String encrypted = aiConfigService.encrypt(plaintext);
        String decrypted = aiConfigService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    // ── getConfigEntity ──────────────────────────────────────

    @Test
    void getConfigEntity_shouldReturnExisting() {
        when(aiConfigRepository.findSingleConfig()).thenReturn(Optional.of(defaultConfig));

        AiConfig result = aiConfigService.getConfigEntity();

        assertEquals("OLLAMA", result.getLlmProvider());
    }

    @Test
    void getConfigEntity_shouldCreateDefaultWhenMissing() {
        when(aiConfigRepository.findSingleConfig()).thenReturn(Optional.empty());
        when(aiConfigRepository.save(any(AiConfig.class))).thenReturn(defaultConfig);

        AiConfig result = aiConfigService.getConfigEntity();

        assertNotNull(result);
        verify(aiConfigRepository).save(any(AiConfig.class));
    }

    // ── buildEmbeddingModel ───────────────────────────────────

    @Test
    void buildEmbeddingModel_shouldBuildOllamaModel() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("OLLAMA");
        props.setEndpointUrl("http://localhost:11434");
        props.setModel("nomic-embed-text");

        EmbeddingModel result = aiConfigService.buildEmbeddingModel(props);

        assertNotNull(result);
        assertInstanceOf(OllamaEmbeddingModel.class, result);
    }

    @Test
    void buildEmbeddingModel_shouldBuildOpenAiModel() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("OPENAI_COMPATIBLE");
        props.setEndpointUrl("https://api.voyageai.com/v1/");
        props.setModel("voyage-4-lite");
        props.setApiKey("test-api-key");

        EmbeddingModel result = aiConfigService.buildEmbeddingModel(props);

        assertNotNull(result);
        assertInstanceOf(OpenAiCompatibleEmbeddingModel.class, result);
    }

    @Test
    void buildEmbeddingModel_shouldFallBackToOllamaWhenNoApiKey() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("OPENAI_COMPATIBLE");
        props.setEndpointUrl("https://api.voyageai.com/v1/");
        props.setModel("voyage-4-lite");
        props.setApiKey("");

        EmbeddingModel result = aiConfigService.buildEmbeddingModel(props);

        assertNotNull(result);
        assertInstanceOf(OllamaEmbeddingModel.class, result);
    }
}
