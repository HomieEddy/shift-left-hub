package com.shiftleft.hub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the embedding provider.
 * All values are read from environment variables, never from the database.
 * Defaults to local Ollama for development.
 */
@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

    private String provider = "OLLAMA";

    private String endpointUrl = "http://host.docker.internal:11434";

    private String model = "nomic-embed-text";

    private String apiKey = "";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
