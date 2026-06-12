package com.shiftleft.hub.document.service;

import com.shiftleft.hub.document.domain.DocumentChunk;
import com.shiftleft.hub.document.domain.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEmbeddingServiceTest {

    @Mock private EmbeddingModel embeddingModel;
    @Mock private DocumentChunkRepository documentChunkRepository;

    @InjectMocks private DocumentEmbeddingService documentEmbeddingService;

    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    private DocumentChunk createChunk(int index, String content) {
        return DocumentChunk.builder()
            .id(UUID.randomUUID())
            .documentId(DOCUMENT_ID)
            .content(content)
            .chunkIndex(index)
            .build();
    }

    @Test
    void generateEmbeddings_shouldWarnAndSkipWhenChunksEmpty() {
        documentEmbeddingService.generateEmbeddings(DOCUMENT_ID, List.of());

        verify(embeddingModel, never()).embed(anyList());
        verify(documentChunkRepository, never()).findByDocumentIdOrderByChunkIndexAsc(any());
        verify(documentChunkRepository, never()).saveAll(anyList());
    }

    @Test
    void generateEmbeddings_shouldGenerateEmbeddingsForAllChunks() {
        List<String> chunkContents = List.of("First chunk content", "Second chunk content");
        List<DocumentChunk> chunks = List.of(
            createChunk(0, "First chunk content"),
            createChunk(1, "Second chunk content")
        );

        float[] embedding1 = {0.1f, 0.2f, 0.3f};
        float[] embedding2 = {0.4f, 0.5f, 0.6f};
        when(embeddingModel.embed(chunkContents)).thenReturn(List.of(embedding1, embedding2));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(DOCUMENT_ID))
            .thenReturn(chunks);

        documentEmbeddingService.generateEmbeddings(DOCUMENT_ID, chunkContents);

        verify(embeddingModel).embed(chunkContents);
        verify(documentChunkRepository).findByDocumentIdOrderByChunkIndexAsc(DOCUMENT_ID);
        verify(documentChunkRepository).saveAll(chunks);

        assertArrayEquals(embedding1, chunks.get(0).getEmbedding(), 0.001f);
        assertArrayEquals(embedding2, chunks.get(1).getEmbedding(), 0.001f);
    }

    @Test
    void generateEmbeddings_shouldHandleMoreChunksThanEmbeddings() {
        List<String> chunkContents = List.of("Chunk 1", "Chunk 2", "Chunk 3");
        List<DocumentChunk> chunks = List.of(
            createChunk(0, "Chunk 1"),
            createChunk(1, "Chunk 2"),
            createChunk(2, "Chunk 3")
        );

        // Only 2 embeddings returned for 3 chunks
        float[] embedding1 = {0.1f, 0.2f};
        float[] embedding2 = {0.3f, 0.4f};
        when(embeddingModel.embed(chunkContents)).thenReturn(List.of(embedding1, embedding2));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(DOCUMENT_ID))
            .thenReturn(chunks);

        documentEmbeddingService.generateEmbeddings(DOCUMENT_ID, chunkContents);

        verify(embeddingModel).embed(chunkContents);
        verify(documentChunkRepository).saveAll(chunks);

        // First two chunks should have embeddings, third should not
        assertArrayEquals(embedding1, chunks.get(0).getEmbedding(), 0.001f);
        assertArrayEquals(embedding2, chunks.get(1).getEmbedding(), 0.001f);
        assertArrayEquals(null, chunks.get(2).getEmbedding());
    }

    @Test
    void generateEmbeddings_shouldHandleMoreEmbeddingsThanChunks() {
        List<String> chunkContents = List.of("Single chunk");
        List<DocumentChunk> chunks = List.of(createChunk(0, "Single chunk"));

        float[] embedding1 = {0.1f, 0.2f, 0.3f};
        float[] embedding2 = {0.4f, 0.5f, 0.6f}; // Extra embedding not used
        when(embeddingModel.embed(chunkContents)).thenReturn(List.of(embedding1, embedding2));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(DOCUMENT_ID))
            .thenReturn(chunks);

        documentEmbeddingService.generateEmbeddings(DOCUMENT_ID, chunkContents);

        verify(embeddingModel).embed(chunkContents);
        verify(documentChunkRepository).saveAll(chunks);

        assertArrayEquals(embedding1, chunks.get(0).getEmbedding(), 0.001f);
    }

    @Test
    void generateEmbeddings_shouldHandleEmptyEmbeddingVectors() {
        List<String> chunkContents = List.of("Chunk content");
        List<DocumentChunk> chunks = List.of(createChunk(0, "Chunk content"));

        float[] emptyEmbedding = {};
        when(embeddingModel.embed(chunkContents)).thenReturn(List.of(emptyEmbedding));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(DOCUMENT_ID))
            .thenReturn(chunks);

        documentEmbeddingService.generateEmbeddings(DOCUMENT_ID, chunkContents);

        verify(documentChunkRepository).saveAll(chunks);
        assertArrayEquals(emptyEmbedding, chunks.get(0).getEmbedding(), 0.001f);
    }
}
