package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.document.domain.DocumentChunkRepository;
import com.shiftleft.hub.document.domain.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedSearchServiceTest {

    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private EmbeddingModel embeddingModel;

    @InjectMocks private UnifiedSearchService unifiedSearchService;

    private final UUID workspaceId = UUID.randomUUID();

    @Test
    void ftsSearchDocumentChunks_shouldReturnResults() {
        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Object[] row = {chunkId, documentId, "test content", 0, "test.txt", "text/plain"};
        when(documentChunkRepository.ftsSearch("test", workspaceId, 10)).thenReturn(List.of(row));

        var results = unifiedSearchService.ftsSearchDocumentChunks("test", workspaceId);

        assertEquals(1, results.size());
        assertEquals("test.txt", results.getFirst().filename());
        assertEquals(chunkId, results.getFirst().chunkId());
    }

    @Test
    void ftsSearchDocumentChunks_shouldReturnEmptyWhenNoResults() {
        when(documentChunkRepository.ftsSearch("test", workspaceId, 10)).thenReturn(List.of());

        var results = unifiedSearchService.ftsSearchDocumentChunks("test", workspaceId);

        assertTrue(results.isEmpty());
    }

    @Test
    void vectorSearchDocumentChunks_shouldReturnResults() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed("test query")).thenReturn(embedding);

        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Object[] row = {chunkId, documentId, "test content", 0, "doc.pdf", 0.85};
        when(documentChunkRepository.vectorSearch(eq(embedding), eq(workspaceId), eq(0.65), eq(10)))
            .thenReturn(List.of(row));

        var results = unifiedSearchService.vectorSearchDocumentChunks("test query", workspaceId, 0.65);

        assertEquals(1, results.size());
        assertEquals("doc.pdf", results.getFirst().filename());
        assertEquals(0.85, results.getFirst().score(), 0.001);
    }

    @Test
    void vectorSearchDocumentChunks_shouldReturnEmptyOnEmbeddingFailure() {
        when(embeddingModel.embed("test")).thenThrow(new RuntimeException("API error"));

        var results = unifiedSearchService.vectorSearchDocumentChunks("test", workspaceId, 0.65);

        assertTrue(results.isEmpty());
    }

    @Test
    void vectorSearchDocumentChunks_shouldTruncateLongExcerpt() {
        String longContent = "x".repeat(300);
        float[] embedding = {0.1f};
        when(embeddingModel.embed("query")).thenReturn(embedding);

        Object[] row = {UUID.randomUUID(), UUID.randomUUID(), longContent, 0, "long.txt", 0.9};
        when(documentChunkRepository.vectorSearch(any(), eq(workspaceId), eq(0.65), eq(10)))
            .thenReturn(List.of(row));

        var results = unifiedSearchService.vectorSearchDocumentChunks("query", workspaceId, 0.65);

        assertEquals(203, results.getFirst().excerpt().length());
        assertTrue(results.getFirst().excerpt().endsWith("..."));
    }

    @Test
    void documentChunkResult_shouldConstructCorrectly() {
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        var result = new UnifiedSearchService.DocumentChunkResult(
            chunkId, docId, "test.pdf", "content", "excerpt", 0, 0.95);

        assertEquals(chunkId, result.chunkId());
        assertEquals(docId, result.documentId());
        assertEquals("test.pdf", result.filename());
        assertEquals("content", result.content());
        assertEquals("excerpt", result.excerpt());
        assertEquals(0, result.chunkIndex());
        assertEquals(0.95, result.score(), 0.001);
    }
}