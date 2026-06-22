package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConfigSeederTest {

    @Mock private AiConfigRepository aiConfigRepository;

    private AiConfigSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new AiConfigSeeder(aiConfigRepository);
    }

    @Test
    void seedAiConfig_createsDefaultWhenNoneExists() {
        when(aiConfigRepository.count()).thenReturn(0L);

        seeder.seedAiConfig();

        verify(aiConfigRepository).save(any(AiConfig.class));
    }

    @Test
    void seedAiConfig_skipsWhenAlreadyExists() {
        when(aiConfigRepository.count()).thenReturn(1L);

        seeder.seedAiConfig();

        verify(aiConfigRepository, never()).save(any(AiConfig.class));
    }
}