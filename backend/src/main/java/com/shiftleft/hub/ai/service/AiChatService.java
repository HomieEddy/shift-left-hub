package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.ai.api.dto.ChatRequest;
import com.shiftleft.hub.ai.api.dto.StreamEvent;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.llmconfig.service.WorkspaceChatModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final WorkspaceChatModelRegistry workspaceChatModelRegistry;
    private final UnifiedSearchService unifiedSearchService;

    private static final int RRF_K = 60;
    private static final int MAX_HISTORY = 10;
    private static final int TOP_K = 10;
    private static final int MAX_CONTEXT_RESULTS = 5;

    record HybridSearchResult(UUID articleId, String titleEn, String titleFr,
        String slug, String excerpt, double score) {
    }

    /**
     * Processes a chat request and streams the response to the SSE emitter.
     *
     * @param request the chat request with message and optional history
     * @param emitter the SSE emitter for streaming the response
     * @param userId  the authenticated user identifier
     */
    public void processChat(ChatRequest request, SseEmitter emitter, String userId) {
        try {
            UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
            var wsConfig = workspaceChatModelRegistry.getWorkspaceConfig(workspaceId);
            double threshold = wsConfig != null
                ? wsConfig.getSimilarityThreshold()
                : aiConfigService.getConfigEntity().getSimilarityThreshold();

            List<HybridSearchResult> results = hybridSearch(request.message(), threshold);

            if (results.isEmpty()) {
                StreamEvent fallbackEvent = new StreamEvent("fallback",
                    "I couldn't find a resolution in the knowledge base. Would you like to escalate to a human agent?",
                    null);
                emitter.send(SseEmitter.event().name("message").data(fallbackEvent));
                emitter.complete();
                return;
            }

            List<HybridSearchResult> topResults = results.size() > MAX_CONTEXT_RESULTS
                ? results.subList(0, MAX_CONTEXT_RESULTS) : results;
            String context = buildContext(topResults);
            String history = formatHistory(request.history());

            ChatClient chatClient = workspaceChatModelRegistry.getChatClient(workspaceId);
            String systemPrompt = workspaceChatModelRegistry.getSystemPrompt(workspaceId);
            String fullPrompt = buildPrompt(request.message(), context, history, systemPrompt);
            AtomicReference<String> fullResponse = new AtomicReference<>("");
            final Disposable[] subscription = {null};

            var flux = chatClient.prompt()
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
                    } finally {
                        if (subscription[0] != null) {
                            subscription[0].dispose();
                        }
                    }
                })
                .doOnComplete(() -> {
                    try {
                        List<StreamEvent.SourceRef> sourceRefs = topResults.stream()
                            .map(r -> new StreamEvent.SourceRef(
                            r.articleId(),
                            r.titleEn() != null ? r.titleEn() : r.titleFr(),
                            r.slug(),
                            r.score(),
                            r.slug() == null ? "document" : null,
                            r.excerpt()))
                            .toList();
                        emitter.send(SseEmitter.event().name("message")
                            .data(new StreamEvent("done", fullResponse.get(), sourceRefs)));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    } finally {
                        if (subscription[0] != null) {
                            subscription[0].dispose();
                        }
                    }
                });

            subscription[0] = flux.subscribe(
                chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        fullResponse.updateAndGet(s -> s + chunk);
                        try {
                            emitter.send(SseEmitter.event().name("message")
                                .data(new StreamEvent("token", chunk, null)));
                        } catch (IOException e) {
                            log.debug("Client disconnected, aborting stream");
                            if (subscription[0] != null) {
                                subscription[0].dispose();
                            }
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
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();

        // 1. FTS search — articles (existing)
        final List<HybridSearchResult> ftsResults = ftsSearch(query);

        // 2. Vector search — articles (existing)
        List<HybridSearchResult> vectorResults;
        try {
            vectorResults = vectorSearch(query, threshold);
        } catch (Exception e) {
            log.warn("Vector search failed, continuing with FTS-only results: {}", e.getMessage());
            vectorResults = List.of();
        }

        // 3. Document chunk vector search
        List<HybridSearchResult> docChunkResults;
        try {
            var chunkResults = unifiedSearchService.vectorSearchDocumentChunks(query, workspaceId, threshold);
            docChunkResults = chunkResults.stream()
                .map(cr -> new HybridSearchResult(
                    cr.chunkId(),
                    cr.filename(),
                    null,
                    null,
                    cr.excerpt(),
                    cr.score()))
                .toList();
        } catch (Exception e) {
            log.warn("Document chunk vector search failed: {}", e.getMessage());
            docChunkResults = List.of();
        }

        // 4. Document chunk FTS search
        List<HybridSearchResult> docChunkFtsResults;
        try {
            var chunkFtsResults = unifiedSearchService.ftsSearchDocumentChunks(query, workspaceId);
            docChunkFtsResults = chunkFtsResults.stream()
                .map(cr -> new HybridSearchResult(
                    cr.chunkId(),
                    cr.filename(),
                    null,
                    null,
                    cr.excerpt(),
                    0))
                .toList();
        } catch (Exception e) {
            log.warn("Document chunk FTS search failed: {}", e.getMessage());
            docChunkFtsResults = List.of();
        }

        // Four-way RRF merge: FTS articles + vector articles + vector doc chunks + FTS doc chunks
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

        for (int k = 0; k < docChunkResults.size(); k++) {
            HybridSearchResult r = docChunkResults.get(k);
            rrfScores.merge(r.articleId(), 1.0 / (RRF_K + k), Double::sum);
            resultMap.putIfAbsent(r.articleId(), r);
        }

        for (int l = 0; l < docChunkFtsResults.size(); l++) {
            HybridSearchResult r = docChunkFtsResults.get(l);
            rrfScores.merge(r.articleId(), 1.0 / (RRF_K + l), Double::sum);
            resultMap.putIfAbsent(r.articleId(), r);
        }

        return rrfScores.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .map(e -> {
                HybridSearchResult r = resultMap.get(e.getKey());
                return new HybridSearchResult(
                    r.articleId(), r.titleEn(), r.titleFr(),
                    r.slug(), r.excerpt(), e.getValue());
            })
            .toList();
    }

    private List<HybridSearchResult> ftsSearch(String query) {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        var page = articleRepository.searchByText(query, workspaceId, PageRequest.of(0, TOP_K));
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
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(TOP_K)
                .similarityThreshold(threshold)
                .filterExpression(new FilterExpressionBuilder().eq("workspace_id", workspaceId.toString()).build())
                .build());
        return docs.stream()
            .map(doc -> {
                Map<String, Object> meta = doc.getMetadata();
                if (!meta.containsKey("articleId")) {
                    log.warn("Vector result missing articleId, skipping");
                    return null;
                }
                UUID articleId = UUID.fromString((String) meta.get("articleId"));
                String title = (String) meta.getOrDefault("title", "");
                String slug = (String) meta.getOrDefault("slug", "");
                double score = doc.getScore() != null ? doc.getScore() : 0;
                return new HybridSearchResult(articleId, title, "", slug, "", score);
            })
            .filter(Objects::nonNull)
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
        if (history == null || history.isEmpty()) {
            return "";
        }
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

    private String buildPrompt(String userMessage, String context, String history, String systemPrompt) {
        String effectivePrompt = (systemPrompt != null && !systemPrompt.isBlank())
            ? systemPrompt
            : "You are a helpful assistant using the workspace knowledge base. Answer based on the"
            + " workspace's knowledge base articles and uploaded documents. Use Markdown formatting"
            + " with clear sections.";

        effectivePrompt = effectivePrompt
            .replace("{workspace_name}", resolveWorkspaceName())
            // TODO: Resolve {domain} and {categories} from workspace configuration
            // when workspace domain/category context is available
            .replace("{domain}", "")
            .replace("{categories}", "");

        return """
    %s

    %s

    Conversation history:
    %s

    User: %s
    Assistant:""".formatted(effectivePrompt, context, history, userMessage);
    }

    private String resolveWorkspaceName() {
        UUID wsId = WorkspaceContextHolder.getCurrentWorkspaceId();
        // TODO: resolve actual workspace name from WorkspaceRepository
        // Currently falls back to UUID string when proper name resolution is not available
        return wsId != null ? wsId.toString() : "this workspace";
    }
}
