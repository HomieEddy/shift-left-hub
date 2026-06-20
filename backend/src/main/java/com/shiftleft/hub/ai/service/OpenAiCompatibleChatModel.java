package com.shiftleft.hub.ai.service;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
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

    private final OpenAIOkHttpClient openAiClient;
    private final String model;

    public OpenAiCompatibleChatModel(String endpointUrl, String apiKey, String model) {
        this(buildClient(endpointUrl, apiKey), model);
    }

    OpenAiCompatibleChatModel(OpenAIOkHttpClient openAiClient, String model) {
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
            generations.add(new Generation(msg, ChatGenerationMetadata.builder()
                .finishReason(choice.finishReason().toString())
                .build()));
        }

        return new ChatResponse(generations, ChatResponseMetadata.builder()
            .model(model)
            .build());
    }

    @Override
    public ChatResponse call(org.springframework.ai.model.ModelRequest request) {
        if (request instanceof Prompt prompt) {
            return call(prompt);
        }
        throw new UnsupportedOperationException("Unsupported request type: " + request.getClass());
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
                        generations.add(new Generation(msg, ChatGenerationMetadata.builder()
                            .finishReason(choice.finishReason().isPresent()
                                ? choice.finishReason().get().toString() : null)
                            .build()));
                    }
                    return new ChatResponse(generations, ChatResponseMetadata.builder()
                        .model(model)
                        .build());
                })
        );
    }

    @Override
    public reactor.core.publisher.Flux<ChatResponse> stream(org.springframework.ai.model.ModelRequest request) {
        if (request instanceof Prompt prompt) {
            return stream(prompt);
        }
        throw new UnsupportedOperationException("Unsupported request type: " + request.getClass());
    }

    private ChatCompletionCreateParams buildParams(Prompt prompt) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        for (Message msg : prompt.getInstructions()) {
            if (msg instanceof UserMessage userMsg) {
                messages.add(ChatCompletionUserMessageParam.builder()
                    .content(userMsg.getText())
                    .build());
            } else if (msg instanceof AssistantMessage assistantMsg) {
                messages.add(ChatCompletionAssistantMessageParam.builder()
                    .content(assistantMsg.getText())
                    .build());
            } else if (msg instanceof org.springframework.ai.chat.messages.SystemMessage sysMsg) {
                messages.add(ChatCompletionSystemMessageParam.builder()
                    .content(sysMsg.getText())
                    .build());
            }
        }

        return ChatCompletionCreateParams.builder()
            .model(model)
            .messages(messages)
            .build();
    }

    private static OpenAIOkHttpClient buildClient(String endpointUrl, String apiKey) {
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
