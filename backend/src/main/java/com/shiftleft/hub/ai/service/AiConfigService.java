package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.ai.api.dto.AiConfigRequest;
import com.shiftleft.hub.ai.api.dto.AiConfigResponse;
import com.shiftleft.hub.ai.api.dto.TestConnectionResult;
import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.ai.encryption-key}")
    private String encryptionKey;

    public AiConfigResponse getConfig() {
        AiConfig config = getConfigEntity();
        return AiConfigResponse.from(config);
    }

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

    @Transactional
    public TestConnectionResult testConnection(AiConfigRequest request) {
        try {
            String provider = request.llmProvider() != null ? request.llmProvider() : "OLLAMA";
            String model = request.chatModelName() != null ? request.chatModelName() : "llama3.2:3b";
            String endpointUrl = request.ollamaEndpointUrl() != null ? request.ollamaEndpointUrl() : "http://host.docker.internal:11434";
            String apiKey = request.openaiApiKey();

            ChatModel chatModel;
            if ("OPENAI".equals(provider) && apiKey != null && !apiKey.isBlank()) {
                chatModel = OpenAiChatModel.builder()
                    .openAiClient(OpenAIOkHttpClient.builder().apiKey(apiKey).build())
                    .options(OpenAiChatOptions.builder().model(model).build())
                    .build();
            } else {
                chatModel = OllamaChatModel.builder()
                    .ollamaApi(OllamaApi.builder().baseUrl(endpointUrl).build())
                    .defaultOptions(OllamaChatOptions.builder().model(model).build())
                    .build();
            }

            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String response = chatClient.prompt()
                .user("Return only the word hello.")
                .call()
                .content();

            return new TestConnectionResult(true,
                "Connection successful. Model " + model + " responded: " + (response != null ? response.trim() : ""));
        } catch (Exception e) {
            log.warn("Connection test failed: {}", e.getMessage());
            return new TestConnectionResult(false, "Connection failed: " + e.getMessage());
        }
    }

    String encrypt(String plaintext) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(
                encryptionKey.getBytes("UTF-8"));
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    String decrypt(String ciphertext) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(
                encryptionKey.getBytes("UTF-8"));
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(ciphertext));
            byte[] iv = new byte[12];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return new String(cipher.doFinal(encrypted), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
