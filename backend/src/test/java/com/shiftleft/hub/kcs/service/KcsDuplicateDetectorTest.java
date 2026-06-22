package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KcsDuplicateDetectorTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private VectorStore vectorStore;

    private KcsDuplicateDetector detector;

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(WORKSPACE_ID);
        detector = new KcsDuplicateDetector(articleRepository, vectorStore);
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    private TicketResolvedEvent event() {
        return new TicketResolvedEvent(
            UUID.randomUUID(), "TKT-0001",
            "Cannot connect to corporate VPN",
            "{\"chat\":\"...\"}",
            TicketCategory.NETWORK, TicketUrgency.HIGH,
            "Reset VPN credentials",
            "Agent", "agent@example.com",
            "User", LocalDateTime.now()
        );
    }

    @Test
    void checkDuplicates_returnsFtsMatches() {
        UUID match = UUID.randomUUID();
        Page<UUID> page = new PageImpl<>(List.of(match));
        when(articleRepository.searchIdsByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        Set<UUID> duplicates = detector.checkDuplicates(event());

        assertTrue(duplicates.contains(match));
    }

    @Test
    void checkDuplicates_unionsFtsAndSemanticMatches() {
        UUID ftsMatch = UUID.randomUUID();
        UUID semanticMatch = UUID.randomUUID();
        Page<UUID> page = new PageImpl<>(List.of(ftsMatch));
        when(articleRepository.searchIdsByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);
        Document doc = new Document("content", Map.of("articleId", semanticMatch.toString()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        Set<UUID> duplicates = detector.checkDuplicates(event());

        assertEquals(2, duplicates.size());
        assertTrue(duplicates.contains(ftsMatch));
        assertTrue(duplicates.contains(semanticMatch));
    }

    @Test
    void checkDuplicates_swallowsVectorStoreException() {
        Page<UUID> page = new PageImpl<>(List.of());
        when(articleRepository.searchIdsByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenThrow(new RuntimeException("vector store down"));

        // FTS phase succeeds; vector phase fails — detector should not propagate.
        Set<UUID> duplicates = detector.checkDuplicates(event());

        assertNotNull(duplicates);
        assertTrue(duplicates.isEmpty());
    }

    @Test
    void checkDuplicates_skipsMalformedArticleIdInMetadata() {
        Page<UUID> page = new PageImpl<>(List.of());
        when(articleRepository.searchIdsByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);
        Document good = new Document("content", Map.of("articleId", UUID.randomUUID().toString()));
        Document bad = new Document("content", Map.of("articleId", "not-a-uuid"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(good, bad));

        Set<UUID> duplicates = detector.checkDuplicates(event());

        assertEquals(1, duplicates.size());
    }

    @Test
    void checkDuplicates_handlesNullEventFieldsGracefully() {
        TicketResolvedEvent ev = new TicketResolvedEvent(
            UUID.randomUUID(), "TKT-0001",
            null, null,
            TicketCategory.NETWORK, TicketUrgency.LOW,
            null, null, null, null, LocalDateTime.now()
        );
        Page<UUID> page = new PageImpl<>(List.of());
        when(articleRepository.searchIdsByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // Should not throw NPE on null event fields.
        Set<UUID> duplicates = detector.checkDuplicates(ev);
        assertNotNull(duplicates);
    }
}
