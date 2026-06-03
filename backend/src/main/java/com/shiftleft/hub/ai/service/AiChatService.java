package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.ai.api.dto.ChatRequest;
import com.shiftleft.hub.ai.api.dto.StreamEvent;
import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiChatService {

    private final ArticleRepository articleRepository;
    private final VectorStore vectorStore;
    private final AiConfigService aiConfigService;

    private static final int RRF_K = 60;
    private static final int MAX_HISTORY = 10;
    private static final int TOP_K = 10;

    record HybridSearchResult(UUID articleId, String titleEn, String titleFr, String slug, String excerpt, double score) {}

    public void processChat(ChatRequest request, SseEmitter emitter, String userId) {
        try {
            AiConfig config = aiConfigService.getConfigEntity();
            double threshold = config.getSimilarityThreshold();

            List<HybridSearchResult> results = hybridSearch(request.message(), threshold);

            if (results.isEmpty()) {
                StreamEvent fallbackEvent = new StreamEvent("fallback",
                    "I couldn't find a resolution in the knowledge base. Would you like to escalate to a human agent?",
                    null);
                emitter.send(SseEmitter.event().name("message").data(fallbackEvent));
                emitter.complete();
                return;
            }

            List<HybridSearchResult> topResults = results.size() > 5 ? results.subList(0, 5) : results;
            String context = buildContext(topResults);
            String history = formatHistory(request.history());
            String fullPrompt = buildPrompt(request.message(), context, history);

            ChatClient chatClient = buildChatClient(config);
            AtomicReference<String> fullResponse = new AtomicReference<>("");

            chatClient.prompt()
                .user(fullPrompt)
                .stream()
                .content()
                .doOnError(error -> {
                    try {
                        emitter.send(SseEmitter.event().name("message")
                            .data(new StreamEvent("error", "Stream error: " + error.getMessage(), null)));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        List<StreamEvent.SourceRef> sourceRefs = topResults.stream()
                            .map(r -> new StreamEvent.SourceRef(r.articleId(), r.titleEn() != null ? r.titleEn() : r.titleFr(), r.slug(), r.score()))
                            .toList();
                        emitter.send(SseEmitter.event().name("message")
                            .data(new StreamEvent("done", fullResponse.get(), sourceRefs)));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .subscribe(
                    chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            fullResponse.updateAndGet(s -> s + chunk);
                            try {
                                emitter.send(SseEmitter.event().name("message")
                                    .data(new StreamEvent("token", chunk, null)));
                            } catch (IOException e) {
                                log.debug("Client disconnected, aborting stream");
                                emitter.completeWithError(e);
                            }
                        }
                    }
                );

        } catch (Exception e) {
            log.error("Error processing chat: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("message")
                    .data(new StreamEvent("error", "An error occurred: " + e.getMessage(), null)));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    List<HybridSearchResult> hybridSearch(String query, double threshold) {
        List<HybridSearchResult> ftsResults = ftsSearch(query);
        List<HybridSearchResult> vectorResults;
        try {
            vectorResults = vectorSearch(query, threshold);
        } catch (Exception e) {
            log.warn("Vector search failed, continuing with FTS-only results: {}", e.getMessage());
            vectorResults = List.of();
        }

        Map<UUID, Double> rrfScores = new HashMap<>();
        Map<UUID, HybridSearchResult> resultMap = new HashMap<>();

        for (int i = 0; i < ftsResults.size(); i++) {
            HybridSearchResult r = ftsResults.get(i);
            rrfScores.merge(r.articleId(), 1.0 / (RRF_K + i), Double::sum);
            resultMap.putIfAbsent(r.articleId(), r);
        }

        for (int j = 0; j < vectorResults.size(); j++) {
            HybridSearchResult r = vectorResults.get(j);
            rrfScores.merge(r.articleId(), 1.0 / (RRF_K + j), Double::sum);
            resultMap.putIfAbsent(r.articleId(), r);
        }

        return rrfScores.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .map(e -> {
                HybridSearchResult r = resultMap.get(e.getKey());
                return new HybridSearchResult(r.articleId(), r.titleEn(), r.titleFr(), r.slug(), r.excerpt(), e.getValue());
            })
            .toList();
    }

    private ChatClient buildChatClient(AiConfig config) {
        String modelName = config.getChatModelName() != null ? config.getChatModelName() : "llama3.2:3b";
        ChatModel chatModel;

        if ("OPENAI".equals(config.getLlmProvider()) && config.getOpenaiApiKey() != null && !config.getOpenaiApiKey().isEmpty()) {
            String decryptedKey = aiConfigService.decrypt(config.getOpenaiApiKey());
            chatModel = OpenAiChatModel.builder()
                .openAiClient(OpenAIOkHttpClient.builder().apiKey(decryptedKey).build())
                .options(OpenAiChatOptions.builder().model(modelName).build())
                .build();
        } else {
            String baseUrl = config.getOllamaEndpointUrl() != null ? config.getOllamaEndpointUrl() : "http://host.docker.internal:11434";
            chatModel = OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl(baseUrl).build())
                .defaultOptions(OllamaChatOptions.builder().model(modelName).build())
                .build();
        }

        return ChatClient.builder(chatModel).build();
    }

    private List<HybridSearchResult> ftsSearch(String query) {
        var page = articleRepository.searchByText(query, org.springframework.data.domain.PageRequest.of(0, TOP_K));
        return page.getContent().stream()
            .map(row -> {
                UUID id = (UUID) row[0];
                String titleEn = (String) row[1];
                String titleFr = (String) row[2];
                String slug = (String) row[3];
                String excerpt = (String) row[4];
                return new HybridSearchResult(id, titleEn, titleFr, slug, excerpt, 0);
            })
            .toList();
    }

    private List<HybridSearchResult> vectorSearch(String query, double threshold) {
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(TOP_K).similarityThreshold(threshold).build());
        return docs.stream()
            .map(doc -> {
                Map<String, Object> meta = doc.getMetadata();
                UUID articleId = meta.containsKey("articleId")
                    ? UUID.fromString((String) meta.get("articleId"))
                    : UUID.randomUUID();
                String title = (String) meta.getOrDefault("title", "");
                String slug = (String) meta.getOrDefault("slug", "");
                double score = doc.getScore() != null ? doc.getScore() : 0;
                return new HybridSearchResult(articleId, title, "", slug, "", score);
            })
            .toList();
    }

    private String buildContext(List<HybridSearchResult> results) {
        StringBuilder sb = new StringBuilder("Relevant knowledge base articles:\n\n");
        for (HybridSearchResult r : results) {
            String title = r.titleEn() != null ? r.titleEn() : r.titleFr();
            sb.append("--- Article: ").append(title).append(" ---\n");
            if (r.excerpt() != null && !r.excerpt().isEmpty()) {
                sb.append(r.excerpt());
            }
            sb.append("\n[score: ").append(String.format("%.4f", r.score())).append("]\n\n");
        }
        return sb.toString();
    }

    private String formatHistory(List<ChatRequest.ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        List<ChatRequest.ChatMessage> recent = history.size() > MAX_HISTORY
            ? history.subList(history.size() - MAX_HISTORY, history.size())
            : history;
        for (ChatRequest.ChatMessage msg : recent) {
            String role = "user".equals(msg.role()) ? "User" : "Assistant";
            sb.append(role).append(": ").append(msg.content()).append("\n");
        }
        return sb.toString();
    }

    private String buildPrompt(String userMessage, String context, String history) {
        return """
You are an IT support assistant. Use the following knowledge base articles to answer the user's question.
Provide step-by-step resolution guides based on the articles. If the articles don't answer the question,
say you couldn't find relevant information and offer to escalate.

Formatting rules for every response:
- Use Markdown.
- Start with a short summary sentence.
- Use a section titled "### Steps" with a numbered list for actions.
- Use bullet points for notes, prerequisites, warnings, or alternatives.
- Add blank lines between sections and between paragraphs.
- Keep each step concise and on its own line.

%s

Conversation history:
%s

User: %s
Assistant:""".formatted(context, history, userMessage);
    }
}
