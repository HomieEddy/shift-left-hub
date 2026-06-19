package com.shiftleft.hub.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAI-compatible embedding client that does not require usage metadata.
 */
public class OpenAiCompatibleEmbeddingModel extends AbstractEmbeddingModel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String model;

    public OpenAiCompatibleEmbeddingModel(String endpointUrl, String apiKey, String model) {
        this(buildRestClient(endpointUrl, apiKey), model);
    }

    OpenAiCompatibleEmbeddingModel(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public float[] embed(Document document) {
        return embed(getEmbeddingContent(document));
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", request.getInstructions());
        if (request.getOptions() != null && request.getOptions().getDimensions() != null) {
            body.put("dimensions", request.getOptions().getDimensions());
        }

        String response = restClient.post()
            .uri("/embeddings")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String.class);

        return new EmbeddingResponse(parseEmbeddings(response), new EmbeddingResponseMetadata(model, null));
    }

    private List<Embedding> parseEmbeddings(String response) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(response);
        } catch (JacksonException e) {
            throw new IllegalStateException("Embedding response is not valid JSON", e);
        }

        JsonNode data = root != null ? root.get("data") : null;
        if (data == null || !data.isArray()) {
            throw new IllegalStateException("Embedding response is missing data array");
        }

        List<Embedding> embeddings = new ArrayList<>();
        for (JsonNode item : data) {
            JsonNode embeddingNode = item.get("embedding");
            if (embeddingNode == null || !embeddingNode.isArray()) {
                throw new IllegalStateException("Embedding response item is missing embedding array");
            }

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }

            int index = item.has("index") ? item.get("index").asInt() : embeddings.size();
            embeddings.add(new Embedding(vector, index));
        }
        return embeddings;
    }

    private static RestClient buildRestClient(String endpointUrl, String apiKey) {
        return RestClient.builder()
            .baseUrl(normalizeBaseUrl(endpointUrl))
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    private static String normalizeBaseUrl(String endpointUrl) {
        String resolved = endpointUrl == null || endpointUrl.isBlank()
            ? "https://api.openai.com/v1"
            : endpointUrl.trim();
        return resolved.replaceAll("/+$", "");
    }
}
