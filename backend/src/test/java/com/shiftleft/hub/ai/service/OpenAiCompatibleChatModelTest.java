package com.shiftleft.hub.ai.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleChatModelTest {

    @Test
    void call_shouldParseOpenAiCompatibleResponse() {
        OpenAiCompatibleChatModel model = new OpenAiCompatibleChatModel(
            buildMockClient(), "test-model");
        Prompt prompt = new Prompt(List.of(new UserMessage("Hi")));

        ChatResponse response = model.call(prompt);

        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        Generation gen = response.getResult();
        assertEquals("Hello, how can I help?", ((AssistantMessage) gen.getOutput()).getText());
    }

    @Test
    void call_shouldIncludeModelAndMessagesInRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey("test-key")
            .baseUrl("https://api.example.com/v1")
            .responseValidation(false)
            .build();

        server.expect(requestTo("https://api.example.com/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.model").value("my-model"))
            .andExpect(jsonPath("$.messages[0].role").value("user"))
            .andExpect(jsonPath("$.messages[0].content").value("Hello"))
            .andRespond(withSuccess("""
                {
                  "choices": [
                    {"message": {"content": "Hi!"}, "finish_reason": "stop"}
                  ],
                  "model": "my-model"
                }
                """, MediaType.APPLICATION_JSON));

        OpenAiCompatibleChatModel model = new OpenAiCompatibleChatModel(client, "my-model");
        model.call(new Prompt(List.of(new UserMessage("Hello"))));
        server.verify();
    }

    private OpenAIClient buildMockClient() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.com/v1");
        MockRestServiceServer.bindTo(builder).build();
        return OpenAIOkHttpClient.builder()
            .apiKey("test-key")
            .baseUrl("https://api.example.com/v1")
            .responseValidation(false)
            .build();
    }
}
