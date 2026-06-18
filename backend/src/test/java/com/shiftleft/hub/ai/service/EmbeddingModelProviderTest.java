package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.config.EmbeddingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingModelProviderTest {

    @Mock private AiConfigService aiConfigService;
    @Mock private EmbeddingProperties embeddingProperties;

    @InjectMocks private EmbeddingModelProvider provider;

    @Test
    void getEmbeddingModel_shouldBuildOnFirstCall() {
        EmbeddingModel mockModel = mock(OllamaEmbeddingModel.class);
        when(aiConfigService.buildEmbeddingModel(embeddingProperties)).thenReturn(mockModel);

        EmbeddingModel result = provider.getEmbeddingModel();

        assertSame(mockModel, result);
        verify(aiConfigService, times(1)).buildEmbeddingModel(embeddingProperties);
    }

    @Test
    void getEmbeddingModel_shouldReturnCachedOnSecondCall() {
        EmbeddingModel mockModel = mock(OllamaEmbeddingModel.class);
        when(aiConfigService.buildEmbeddingModel(embeddingProperties)).thenReturn(mockModel);

        provider.getEmbeddingModel();
        provider.getEmbeddingModel();

        verify(aiConfigService, times(1)).buildEmbeddingModel(embeddingProperties);
    }

    @Test
    void evict_shouldRebuildOnNextGet() {
        EmbeddingModel firstModel = mock(OllamaEmbeddingModel.class);
        EmbeddingModel secondModel = mock(OllamaEmbeddingModel.class);
        when(aiConfigService.buildEmbeddingModel(embeddingProperties))
            .thenReturn(firstModel).thenReturn(secondModel);

        provider.getEmbeddingModel();
        provider.evict();
        EmbeddingModel result = provider.getEmbeddingModel();

        assertSame(secondModel, result);
        verify(aiConfigService, times(2)).buildEmbeddingModel(embeddingProperties);
    }
}
