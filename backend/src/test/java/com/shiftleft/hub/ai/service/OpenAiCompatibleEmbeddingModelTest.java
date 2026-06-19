package com.shiftleft.hub.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleEmbeddingModelTest {

    @Test
    void embed_shouldParseOpenAiCompatibleResponseWithoutUsageMetadata() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.example/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleEmbeddingModel model = new OpenAiCompatibleEmbeddingModel(builder.build(), "embed-model");

        server.expect(requestTo("https://provider.example/v1/embeddings"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.model").value("embed-model"))
            .andExpect(jsonPath("$.input[0]").value("hello"))
            .andRespond(withSuccess("""
                {
                  "object": "list",
                  "data": [
                    {
                      "object": "embedding",
                      "index": 0,
                      "embedding": [0.1, 0.2, 0.3]
                    }
                  ],
                  "model": "embed-model"
                }
                """, MediaType.APPLICATION_JSON));

        float[] embedding = model.embed("hello");

        assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, embedding);
        server.verify();
    }

    @Test
    void call_shouldSendAuthorizationHeaderAndParseMultipleEmbeddings() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("https://provider.example/v1")
            .defaultHeader("Authorization", "Bearer test-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleEmbeddingModel model = new OpenAiCompatibleEmbeddingModel(builder.build(), "embed-model");

        server.expect(requestTo("https://provider.example/v1/embeddings"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(jsonPath("$.input[0]").value("first"))
            .andExpect(jsonPath("$.input[1]").value("second"))
            .andRespond(withSuccess("""
                {
                  "data": [
                    {"index": 0, "embedding": [0.1, 0.2]},
                    {"index": 1, "embedding": [0.3, 0.4]}
                  ],
                  "model": "embed-model"
                }
                """, MediaType.APPLICATION_JSON));

        EmbeddingResponse response = model.embedForResponse(List.of("first", "second"));

        assertEquals(2, response.getResults().size());
        assertArrayEquals(new float[] {0.1f, 0.2f}, response.getResult().getOutput());
        assertArrayEquals(new float[] {0.3f, 0.4f}, response.getResults().get(1).getOutput());
        server.verify();
    }
}
