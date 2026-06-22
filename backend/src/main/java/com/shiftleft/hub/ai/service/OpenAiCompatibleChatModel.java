package com.shiftleft.hub.ai.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal OpenAI-compatible chat model that uses the OpenAI Java SDK directly.
 * Bypasses Spring AI's OpenAiChatModel credential handling bug in 2.0.0-M8.
 */
public class OpenAiCompatibleChatModel implements ChatModel {

    private final OpenAIClient openAiClient;
    private final String model;

    public OpenAiCompatibleChatModel(String endpointUrl, String apiKey, String model) {
        this(buildClient(endpointUrl, apiKey), model);
    }

    OpenAiCompatibleChatModel(OpenAIClient openAiClient, String model) {
        this.openAiClient = openAiClient;
        this.model = model;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        ChatCompletionCreateParams params = buildParams(prompt);
        ChatCompletion completion = openAiClient.chat().completions().create(params);

        List<Generation> generations = new ArrayList<>();
        for (ChatCompletion.Choice choice : completion.choices()) {
            String content = choice.message().content().orElse("");
            AssistantMessage msg = new AssistantMessage(content);
            generations.add(new Generation(msg, genMetadata(choice.finishReason().toString())));
        }

        return new ChatResponse(generations, responseMetadata());
    }

    @Override
    public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
        ChatCompletionCreateParams params = buildParams(prompt);
        return reactor.core.publisher.Flux.fromStream(
            openAiClient.chat().completions().createStreaming(params).stream()
                .map(chunk -> {
                    List<Generation> generations = new ArrayList<>();
                    for (ChatCompletionChunk.Choice choice : chunk.choices()) {
                        String content = "";
                        if (choice.delta().content().isPresent()) {
                            content = choice.delta().content().get();
                        }
                        AssistantMessage msg = new AssistantMessage(content);
                        String finishReason = choice.finishReason().isPresent()
                            ? choice.finishReason().get().toString() : null;
                        generations.add(new Generation(msg, genMetadata(finishReason)));
                    }
                    return new ChatResponse(generations, responseMetadata());
                })
        );
    }

    private ChatResponseMetadata responseMetadata() {
        return ChatResponseMetadata.builder().model(model).build();
    }

    private ChatGenerationMetadata genMetadata(String finishReason) {
        return ChatGenerationMetadata.builder().finishReason(finishReason).build();
    }

    private ChatCompletionCreateParams buildParams(Prompt prompt) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
            .model(model);

        for (Message msg : prompt.getInstructions()) {
            if (msg instanceof org.springframework.ai.chat.messages.SystemMessage sysMsg) {
                builder.addSystemMessage(sysMsg.getText());
            } else if (msg instanceof UserMessage userMsg) {
                builder.addUserMessage(userMsg.getText());
            } else if (msg instanceof AssistantMessage assistantMsg) {
                builder.addAssistantMessage(assistantMsg.getText());
            }
        }

        return builder.build();
    }

    private static OpenAIClient buildClient(String endpointUrl, String apiKey) {
        return OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(normalizeBaseUrl(endpointUrl))
            .responseValidation(false)
            .build();
    }

    private static String normalizeBaseUrl(String endpointUrl) {
        String resolved = endpointUrl == null || endpointUrl.isBlank()
            ? "https://api.openai.com/v1"
            : endpointUrl.trim();
        return resolved.replaceAll("/+$", "");
    }
}
