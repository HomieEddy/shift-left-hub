package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.ai.api.dto.ChatRequest;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.llmconfig.service.WorkspaceChatModelRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private VectorStore vectorStore;
    @Mock private AiConfigService aiConfigService;
    @Mock private WorkspaceChatModelRegistry workspaceChatModelRegistry;
    @Mock private UnifiedSearchService unifiedSearchService;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec promptSpec;
    @Mock private ChatClient.StreamResponseSpec streamSpec;

    @Captor private ArgumentCaptor<String> promptCaptor;

    @InjectMocks private AiChatService aiChatService;

    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    private com.shiftleft.hub.ai.domain.AiConfig createAiConfig() {
        return com.shiftleft.hub.ai.domain.AiConfig.builder()
            .llmProvider("OLLAMA")
            .ollamaEndpointUrl("http://localhost:11434")
            .chatModelName("llama3.2:3b")
            .embeddingModelName("nomic-embed-text")
            .similarityThreshold(0.65)
            .embeddingDimension(768)
            .build();
    }

    // ── chat ──────────────────────────────────────────────────

    @Test
    void chat_shouldSendFallbackWhenNoSearchResults() throws Exception {
        String query = "nonexistent query";
        ChatRequest request = new ChatRequest(query, null);
        SseEmitter emitter = mock(SseEmitter.class);

        when(aiConfigService.getConfigEntity()).thenReturn(createAiConfig());
        when(articleRepository.searchByText(eq(query), eq(workspaceId), any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(Collections.emptyList()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(unifiedSearchService.vectorSearchDocumentChunks(anyString(), eq(workspaceId), anyDouble()))
            .thenReturn(List.of());
        when(unifiedSearchService.ftsSearchDocumentChunks(anyString(), eq(workspaceId)))
            .thenReturn(List.of());

        aiChatService.processChat(request, emitter, "user1");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void chat_shouldIncludeSearchContextInPrompt() throws Exception {
        UUID articleId = UUID.randomUUID();
        ChatRequest request = new ChatRequest("test query", null);
        SseEmitter emitter = mock(SseEmitter.class);

        Object[] ftsRow = {articleId, "Test Title", "Titre test", "test-article", "Some excerpt",
            LocalDateTime.now()};
        when(aiConfigService.getConfigEntity()).thenReturn(createAiConfig());
        when(articleRepository.searchByText(eq("test query"), eq(workspaceId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.<Object[]>of(ftsRow)));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(unifiedSearchService.vectorSearchDocumentChunks(anyString(), eq(workspaceId), anyDouble()))
            .thenReturn(List.of());
        when(unifiedSearchService.ftsSearchDocumentChunks(anyString(), eq(workspaceId)))
            .thenReturn(List.of());
        when(workspaceChatModelRegistry.getChatClient(workspaceId)).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(promptCaptor.capture())).thenReturn(promptSpec);
        when(promptSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("Hello response"));

        aiChatService.processChat(request, emitter, "user1");

        assertTrue(promptCaptor.getValue().contains("Test Title"));
        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void chat_shouldHandleEmptySearchResults() throws Exception {
        String query = "empty results";
        ChatRequest request = new ChatRequest(query, null);
        SseEmitter emitter = mock(SseEmitter.class);

        when(aiConfigService.getConfigEntity()).thenReturn(createAiConfig());
        when(articleRepository.searchByText(eq(query), eq(workspaceId), any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(Collections.emptyList()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(unifiedSearchService.vectorSearchDocumentChunks(anyString(), eq(workspaceId), anyDouble()))
            .thenReturn(List.of());
        when(unifiedSearchService.ftsSearchDocumentChunks(anyString(), eq(workspaceId)))
            .thenReturn(List.of());

        aiChatService.processChat(request, emitter, "user1");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void chat_shouldThrowWhenWorkspaceNotConfigured() throws Exception {
        WorkspaceContextHolder.clear();
        ChatRequest request = new ChatRequest("hello", null);
        SseEmitter emitter = mock(SseEmitter.class);

        aiChatService.processChat(request, emitter, "user1");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void chat_shouldHandleStreamErrorGracefully() throws Exception {
        UUID articleId = UUID.randomUUID();
        ChatRequest request = new ChatRequest("error test", null);
        SseEmitter emitter = mock(SseEmitter.class);

        Object[] ftsRow = {articleId, "Title", "Titre", "slug", "excerpt",
            LocalDateTime.now()};
        when(aiConfigService.getConfigEntity()).thenReturn(createAiConfig());
        when(articleRepository.searchByText(eq("error test"), eq(workspaceId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.<Object[]>of(ftsRow)));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(unifiedSearchService.vectorSearchDocumentChunks(anyString(), eq(workspaceId), anyDouble()))
            .thenReturn(List.of());
        when(unifiedSearchService.ftsSearchDocumentChunks(anyString(), eq(workspaceId)))
            .thenReturn(List.of());
        when(workspaceChatModelRegistry.getChatClient(workspaceId)).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.error(new RuntimeException("API error")));

        aiChatService.processChat(request, emitter, "user1");

        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }

    // ── hybridSearch ─────────────────────────────────────────

    @Test
    void hybridSearch_shouldReturnEmptyForEmptyFts() {
        when(articleRepository.searchByText(anyString(), eq(workspaceId), any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(Collections.emptyList()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(unifiedSearchService.vectorSearchDocumentChunks(anyString(), eq(workspaceId), anyDouble()))
            .thenReturn(List.of());
        when(unifiedSearchService.ftsSearchDocumentChunks(anyString(), eq(workspaceId)))
            .thenReturn(List.of());

        var results = aiChatService.hybridSearch("test", 0.65);

        assertTrue(results.isEmpty());
    }

    @Test
    void hybridSearch_shouldTolerateVectorSearchFailure() {
        UUID articleId = UUID.randomUUID();
        Object[] ftsRow = {articleId, "Title", "Titre", "slug", "excerpt",
            LocalDateTime.now()};
        when(articleRepository.searchByText(anyString(), eq(workspaceId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.<Object[]>of(ftsRow)));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("Vector store down"));
        when(unifiedSearchService.vectorSearchDocumentChunks(anyString(), eq(workspaceId), anyDouble()))
            .thenReturn(List.of());
        when(unifiedSearchService.ftsSearchDocumentChunks(anyString(), eq(workspaceId)))
            .thenReturn(List.of());

        var results = aiChatService.hybridSearch("test", 0.65);

        assertFalse(results.isEmpty());
    }
}
