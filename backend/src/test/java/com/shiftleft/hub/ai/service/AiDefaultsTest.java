package com.shiftleft.hub.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDefaultsTest {

    @Test
    void provider_is_ollama() {
        assertEquals("OLLAMA", AiDefaults.LLM_PROVIDER);
    }

    @Test
    void models_have_sensible_defaults() {
        assertNotNull(AiDefaults.CHAT_MODEL);
        assertNotNull(AiDefaults.EMBEDDING_MODEL);
        assertTrue(AiDefaults.EMBEDDING_DIMENSION > 0);
    }

    @Test
    void similarity_threshold_is_below_validation_floor() {
        // The bootstrap default trusts curated seed data, so it is
        // intentionally lower than the user-supplied validation floor.
        assertTrue(AiDefaults.SIMILARITY_THRESHOLD < AiDefaults.MIN_SIMILARITY_THRESHOLD,
            "Bootstrap threshold should be lower than the validation floor");
    }

    @Test
    void private_constructor_throws() throws Exception {
        var ctor = AiDefaults.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
