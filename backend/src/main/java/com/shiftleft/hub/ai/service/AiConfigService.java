package com.shiftleft.hub.ai.service;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.shiftleft.hub.ai.api.dto.AiConfigRequest;
import com.shiftleft.hub.ai.api.dto.AiConfigResponse;
import com.shiftleft.hub.ai.api.dto.TestConnectionResult;
import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.ai.encryption-key}")
    private String encryptionKey;

    @Value("${app.ai.encryption-salt}")
    private String encryptionSalt;

    /**
     * Returns the current AI configuration as a response DTO.
     *
     * @return current AI configuration response
     */
    public AiConfigResponse getConfig() {
        AiConfig config = getConfigEntity();
        return AiConfigResponse.from(config);
    }

    /**
     * Returns the current AI configuration entity, creating a default if none exists.
     *
     * @return the AI configuration entity
     */
    public AiConfig getConfigEntity() {
        return aiConfigRepository.findSingleConfig()
            .orElseGet(() -> {
                AiConfig defaultConfig = AiConfig.builder()
                    .llmProvider("OLLAMA")
                    .ollamaEndpointUrl("http://host.docker.internal:11434")
                    .chatModelName("llama3.2:3b")
                    .embeddingModelName("nomic-embed-text")
                    .similarityThreshold(0.65)
                    .embeddingDimension(768)
                    .build();
                return aiConfigRepository.save(defaultConfig);
            });
    }

    /**
     * Updates the AI configuration with non-null fields from the request.
     *
     * @param request the configuration update request
     * @return updated AI configuration response
     */
    @Transactional
    public AiConfigResponse updateConfig(AiConfigRequest request) {
        AiConfig config = getConfigEntity();

        if (request.llmProvider() != null) {
            config.setLlmProvider(request.llmProvider());
        }
        if (request.ollamaEndpointUrl() != null) {
            config.setOllamaEndpointUrl(request.ollamaEndpointUrl());
        }
        if (request.openaiApiKey() != null && !request.openaiApiKey().isBlank()) {
            config.setOpenaiApiKey(encrypt(request.openaiApiKey()));
        }
        if (request.chatModelName() != null) {
            config.setChatModelName(request.chatModelName());
        }
        if (request.embeddingModelName() != null) {
            config.setEmbeddingModelName(request.embeddingModelName());
        }
        if (request.similarityThreshold() != null) {
            config.setSimilarityThreshold(request.similarityThreshold());
        }

        config = aiConfigRepository.save(config);
        return AiConfigResponse.from(config);
    }

    /**
     * Tests a connection to the AI provider with the given configuration.
     *
     * @param request the configuration to test
     * @return connection test result with success flag and message
     */
    @Transactional
    public TestConnectionResult testConnection(AiConfigRequest request) {
        try {
            String provider = request.llmProvider() != null ? request.llmProvider() : "OLLAMA";
            String model = request.chatModelName() != null ? request.chatModelName() : "llama3.2:3b";
            String endpointUrl = request.ollamaEndpointUrl() != null
                ? request.ollamaEndpointUrl() : "http://host.docker.internal:11434";
            String apiKey = request.openaiApiKey();

            String response;
            if (isOpenAiProvider(provider) && apiKey != null && !apiKey.isBlank()) {
                log.info("Testing OpenAI connection: model={}, apiKey length={}", model,
                    apiKey.length());
                var client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
                var params = ChatCompletionCreateParams.builder()
                    .model(model)
                    .addUserMessage("Return only the word hello.")
                    .build();
                var completion = client.chat().completions().create(params);
                response = completion.choices().get(0).message().content().orElse("");
            } else {
                ChatModel chatModel = OllamaChatModel.builder()
                    .ollamaApi(OllamaApi.builder().baseUrl(endpointUrl).build())
                    .defaultOptions(OllamaChatOptions.builder().model(model).build())
                    .build();
                ChatClient chatClient = ChatClient.builder(chatModel).build();
                response = chatClient.prompt()
                    .user("Return only the word hello.")
                    .call()
                    .content();
            }

            return new TestConnectionResult(true,
                "Connection successful. Model " + model + " responded: " + (response != null ? response.trim() : ""));
        } catch (Exception e) {
            log.warn("Connection test failed: {}", e.getMessage());
            return new TestConnectionResult(false, "Connection failed: " + e.getMessage());
        }
    }

    /**
     * Tests a connection with individual provider parameters.
     * Used by WorkspaceLlmConfigService for workspace-scoped test.
     *
     * @param provider    the LLM provider name
     * @param endpointUrl the endpoint URL
     * @param apiKey      the API key (may be encrypted for existing configs)
     * @param modelName   the model name
     * @return connection test result
     */
    public TestConnectionResult testConnection(String provider, String endpointUrl,
            String apiKey, String modelName) {
        try {
            ChatClient chatClient = buildChatClient(provider, endpointUrl, apiKey, modelName);
            String response = chatClient.prompt()
                .user("Return only the word hello.")
                .call()
                .content();
            return new TestConnectionResult(true,
                "Connection successful. Model " + modelName + " responded: "
                    + (response != null ? response.trim() : ""));
        } catch (Exception e) {
            log.warn("Connection test failed: {}", e.getMessage());
            return new TestConnectionResult(false, "Connection failed: " + e.getMessage());
        }
    }

    /**
     * Builds a ChatClient from individual provider parameters.
     * Used by WorkspaceChatModelRegistry for workspace-scoped config.
     *
     * @param provider    the LLM provider name (OPENAI or OLLAMA)
     * @param endpointUrl the endpoint URL (optional, defaults for OLLAMA)
     * @param apiKey      the API key (may be encrypted)
     * @param modelName   the model name
     * @return a configured ChatClient
     */
    public ChatClient buildChatClient(String provider, String endpointUrl, String apiKey, String modelName) {
        String resolvedModel = modelName != null ? modelName : "llama3.2:3b";
        ChatModel chatModel;

        log.info("buildChatClient: provider={}, model={}, apiKeyPresent={}",
            provider, resolvedModel, apiKey != null && !apiKey.isBlank());
        if (isOpenAiProvider(provider) && apiKey != null && !apiKey.isBlank()) {
            String decryptedKey = decrypt(apiKey);
            log.info("buildChatClient: using OpenAI, decryptedKey length={}", decryptedKey.length());
            chatModel = OpenAiChatModel.builder()
                .openAiClient(OpenAIOkHttpClient.builder().apiKey(decryptedKey).build())
                .options(OpenAiChatOptions.builder().model(resolvedModel).build())
                .build();
        } else {
            String baseUrl = endpointUrl != null ? endpointUrl : "http://host.docker.internal:11434";
            log.info("buildChatClient: falling back to Ollama, baseUrl={}", baseUrl);
            chatModel = OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl(baseUrl).build())
                .defaultOptions(OllamaChatOptions.builder().model(resolvedModel).build())
                .build();
        }

        return ChatClient.builder(chatModel).build();
    }

    /**
     * Builds a ChatClient from an AiConfig entity.
     * Kept for backward compatibility.
     *
     * @param config the AI configuration
     * @return a configured ChatClient
     */
    public ChatClient buildChatClient(AiConfig config) {
        String provider = config.getLlmProvider();
        String endpointUrl = config.getOllamaEndpointUrl();
        String apiKey = config.getOpenaiApiKey();
        String modelName = config.getChatModelName();
        return buildChatClient(provider, endpointUrl, apiKey, modelName);
    }

    /**
     * Encrypts a plaintext string using AES/GCM/NoPadding with a PBKDF2-derived key.
     * The initialization vector is prepended to the ciphertext for storage.
     *
     * @param plaintext the plaintext to encrypt
     * @return Base64-encoded ciphertext with IV prepended
     */
    public String encrypt(String plaintext) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(encryptionKey.toCharArray(), getSalt(), 65536, 256);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a ciphertext string using AES/GCM/NoPadding.
     *
     * @param ciphertext the Base64-encoded ciphertext with IV prepended
     * @return the decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(encryptionKey.toCharArray(), getSalt(), 65536, 256);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(ciphertext));
            byte[] iv = new byte[12];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            return new String(cipher.doFinal(encrypted), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private boolean isOpenAiProvider(String provider) {
        return "OPENAI".equals(provider) || "OPENAI_COMPATIBLE".equals(provider);
    }

    private byte[] getSalt() {
        return encryptionSalt.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
