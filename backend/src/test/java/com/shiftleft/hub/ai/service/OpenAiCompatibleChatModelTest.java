package com.shiftleft.hub.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenAiCompatibleChatModelTest {

    @Test
    void constructor_shouldBuildClient() {
        OpenAiCompatibleChatModel model = new OpenAiCompatibleChatModel(
            "https://api.example.com/v1", "test-key", "test-model");

        assertNotNull(model);
    }
}
