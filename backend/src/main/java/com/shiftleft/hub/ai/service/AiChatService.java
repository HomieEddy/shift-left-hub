package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.ai.api.SseEmitterHelper;
import com.shiftleft.hub.ai.api.dto.ChatRequest;
import com.shiftleft.hub.ai.api.dto.StreamEvent;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.service.FtsArticleRow;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.llmconfig.service.WorkspaceChatModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

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
    private static final double FALLBACK_VECTOR_THRESHOLD = 0.35;

    /**
     * Shared executor for the 4 parallel hybrid-search queries in
     * {@link #hybridSearch}. Sized for the typical 4-way fan-out so a
     * single chat request uses 4 threads at most. The pool is shared
     * across all chat requests but bounded so a spike of requests
     * cannot exhaust the host's threads.
     */
    private static final java.util.concurrent.ExecutorService HYBRID_SEARCH_EXECUTOR =
        java.util.concurrent.Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "chat-hybrid-search");
                t.setDaemon(true);
                return t;
            });

    private final java.util.concurrent.ExecutorService hybridSearchExecutor = HYBRID_SEARCH_EXECUTOR;

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
                SseEmitterHelper.emitAndComplete(emitter, new StreamEvent("fallback",
                    "I couldn't find a resolution in the knowledge base. Would you like to escalate to a human agent?",
                    null));
                return;
            }

            List<HybridSearchResult> topResults = results.size() > MAX_CONTEXT_RESULTS
                ? results.subList(0, MAX_CONTEXT_RESULTS) : results;
            String context = buildContext(topResults);
            String history = formatHistory(request.history());

            ChatClient chatClient = workspaceChatModelRegistry.getChatClient(workspaceId);
            String systemPrompt = resolveSystemPrompt(
                workspaceChatModelRegistry.getSystemPrompt(workspaceId));
            String userMessage = buildUserMessage(request.message(), context, history);
            AtomicReference<String> fullResponse = new AtomicReference<>("");
            final Disposable[] subscription = {null};

            var flux = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .doOnError(error -> {
                    try {
                        SseEmitterHelper.emitErrorAndComplete(emitter, "Stream error: " + error.getMessage());
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
                        SseEmitterHelper.emitAndComplete(
                            emitter, new StreamEvent("done", fullResponse.get(), sourceRefs));
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
                        boolean sent = SseEmitterHelper.tryEmit(emitter, new StreamEvent("token", chunk, null));
                        if (!sent) {
                            log.debug("Client disconnected, aborting stream");
                            if (subscription[0] != null) {
                                subscription[0].dispose();
                            }
                        }
                    }
                }
            );

        } catch (Exception e) {
            log.error("Error processing chat: {}", e.getMessage(), e);
            SseEmitterHelper.emitErrorAndComplete(emitter, "An error occurred: " + e.getMessage());
        }
    }

    List<HybridSearchResult> hybridSearch(String query, double threshold) {
        // Capture the workspace id on the calling thread (the
        // WorkspaceContextHolder is ThreadLocal) and pass it explicitly
        // to the parallel search tasks; they run on a different thread
        // and the ThreadLocal would be empty there.
        final UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();

        // Run the 4 independent search queries in parallel. P-8: previously
        // sequential — each query could take 50-200ms; parallel cuts total
        // wall time to roughly max(query) instead of sum(query). Failures
        // are isolated per query (logged + empty list) so a partial outage
        // doesn't break the whole retrieval.
        // Run the 4 independent search queries in parallel. P-8: previously
        // sequential — each query could take 50-200ms; parallel cuts total
        // wall time to roughly max(query) instead of sum(query). Failures
        // are isolated per query (logged + empty list) so a partial outage
        // doesn't break the whole retrieval. WorkspaceContextHolder is a
        // ThreadLocal, so each task re-installs the captured workspaceId
        // on its own thread before running.
        final java.util.UUID capturedWorkspaceId = workspaceId;
        var ftsFuture = java.util.concurrent.CompletableFuture
            .supplyAsync(() -> withWorkspace(capturedWorkspaceId,
                () -> ftsSearch(query, capturedWorkspaceId)), hybridSearchExecutor);
        var vectorFuture = java.util.concurrent.CompletableFuture
            .supplyAsync(() -> withWorkspace(capturedWorkspaceId,
                () -> safeVectorSearch(query, capturedWorkspaceId, threshold)),
                hybridSearchExecutor);
        var docChunkVectorFuture = java.util.concurrent.CompletableFuture
            .supplyAsync(() -> withWorkspace(capturedWorkspaceId,
                () -> safeVectorSearchDocumentChunks(query, capturedWorkspaceId, threshold)),
                hybridSearchExecutor);
        var docChunkFtsFuture = java.util.concurrent.CompletableFuture
            .supplyAsync(() -> withWorkspace(capturedWorkspaceId,
                () -> safeFtsSearchDocumentChunks(query, capturedWorkspaceId)),
                hybridSearchExecutor);

        final List<HybridSearchResult> ftsResults = ftsFuture.join();
        List<HybridSearchResult> vectorResults = vectorFuture.join();
        List<HybridSearchResult> docChunkResults = docChunkVectorFuture.join();
        List<HybridSearchResult> docChunkFtsResults = docChunkFtsFuture.join();

        if (ftsResults.isEmpty()
                && vectorResults.isEmpty()
                && docChunkResults.isEmpty()
                && docChunkFtsResults.isEmpty()
                && threshold > FALLBACK_VECTOR_THRESHOLD) {
            log.info("No chat retrieval results at threshold {}; retrying article vector search at threshold {}",
                threshold, FALLBACK_VECTOR_THRESHOLD);
            try {
                vectorResults = vectorSearch(query, FALLBACK_VECTOR_THRESHOLD);
            } catch (Exception e) {
                log.warn("Fallback vector search failed: {}", e.getMessage());
            }
        }

        log.info(
            "Chat retrieval results: workspace={}, ftsArticles={}, vectorArticles={}, vectorChunks={}, ftsChunks={}",
            workspaceId, ftsResults.size(), vectorResults.size(), docChunkResults.size(), docChunkFtsResults.size());

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

    /**
     * Wraps a {@link java.util.function.Supplier} so the workspace
     * context is re-installed on the current thread before running.
     * WorkspaceContextHolder is a ThreadLocal; the parallel
     * search tasks in {@link #hybridSearch} run on pool threads
     * that have no context, so this helper bridges that gap.
     *
     * @param workspaceId the workspace id to install
     * @param work the work to run
     * @return the work's result
     */
    private <T> T withWorkspace(UUID workspaceId,
            java.util.function.Supplier<T> work) {
        WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
        try {
            return work.get();
        } finally {
            WorkspaceContextHolder.clear();
        }
    }

    /**
     * Wrappers used by the parallel search fan-out in {@link #hybridSearch}.
     * Each wraps a search that can fail and logs + returns empty on
     * failure so a partial outage doesn't break retrieval. The
     * workspaceId is passed explicitly because the parallel tasks
     * run on different threads and the ThreadLocal
     * WorkspaceContextHolder is empty there.
     */
    private List<HybridSearchResult> safeVectorSearch(
            String query, UUID workspaceId, double threshold) {
        try {
            return vectorSearch(query, threshold);
        } catch (Exception e) {
            log.warn("Vector search failed, continuing with empty result: {}", e.getMessage());
            return List.of();
        }
    }

    private List<HybridSearchResult> safeVectorSearchDocumentChunks(
            String query, UUID workspaceId, double threshold) {
        try {
            var chunkResults = unifiedSearchService.vectorSearchDocumentChunks(
                query, workspaceId, threshold);
            return chunkResults.stream()
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
            return List.of();
        }
    }

    private List<HybridSearchResult> safeFtsSearchDocumentChunks(String query, UUID workspaceId) {
        try {
            var chunkFtsResults = unifiedSearchService.ftsSearchDocumentChunks(
                query, workspaceId);
            return chunkFtsResults.stream()
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
            return List.of();
        }
    }

    private List<HybridSearchResult> ftsSearch(String query, UUID workspaceId) {
        var page = articleRepository.searchByText(query, workspaceId, PageRequest.of(0, TOP_K));
        return page.getContent().stream()
            .map(row -> {
                FtsArticleRow r = FtsArticleRow.from(row);
                return new HybridSearchResult(
                    r.id(), r.titleEn(), r.titleFr(), r.slug(), r.excerpt(), 0);
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
                return new HybridSearchResult(articleId, title, "", slug, doc.getText(), score);
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

    private String resolveSystemPrompt(String workspaceSystemPrompt) {
        String prompt = (workspaceSystemPrompt != null && !workspaceSystemPrompt.isBlank())
            ? workspaceSystemPrompt
            : DEFAULT_SYSTEM_PROMPT;
        // The {workspace_name} placeholder is always resolved from the
        // current workspace. The {domain} and {categories} placeholders
        // remain in the template (replaced with empty strings) for
        // backward compatibility with prompts authored before
        // workspace-level domain/category fields were available.
        return prompt
            .replace("{workspace_name}", resolveWorkspaceName())
            .replace("{domain}", "")
            .replace("{categories}", "");
    }

    private String buildUserMessage(String userMessage, String context, String history) {
        StringBuilder sb = new StringBuilder();
        if (!history.isBlank()) {
            sb.append("Conversation history:\n").append(history).append("\n\n");
        }
        if (!context.isBlank()) {
            sb.append(context).append("\n\n");
        }
        sb.append("User question:\n").append(userMessage);
        return sb.toString();
    }

    private String resolveWorkspaceName() {
        return WorkspaceContextHolder.getCurrentWorkspaceId().toString();
    }

    private static final String DEFAULT_SYSTEM_PROMPT = """
        You are a helpful assistant for the {workspace_name} workspace. Your job is to answer
        the user's question using the knowledge base articles and document
        excerpts provided in the user's message below.

        Guidelines:
        - Synthesize a clear, direct answer from the provided context. Do not
          list article titles or links as the answer itself.
        - Use Markdown formatting with headings, bullet points, and short
          paragraphs for readability.
        - If the context is insufficient, say so honestly and suggest the
          user rephrase or escalate to a human agent.
        - Cite article titles inline when you draw a fact from them, so the
          user can find the source in the references list below your answer.
        - Do not invent facts that are not supported by the provided context.
        """;
}
