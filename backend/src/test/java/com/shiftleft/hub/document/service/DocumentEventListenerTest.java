package com.shiftleft.hub.document.service;

import com.shiftleft.hub.document.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEventListenerTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private DocumentParserService documentParserService;
    @Mock private DocumentChunkingService documentChunkingService;
    @Mock private DocumentEmbeddingService documentEmbeddingService;

    @InjectMocks private DocumentEventListener documentEventListener;

    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    private Document createDocument(DocumentStatus status) {
        Document doc = Document.builder()
            .id(DOCUMENT_ID)
            .filename("test-doc.md")
            .mimeType("text/markdown")
            .contentHash("a".repeat(64))
            .status(status)
            .filePath("./uploads/" + WORKSPACE_ID + "/" + DOCUMENT_ID + "/test-doc.md")
            .fileSize(1024L)
            .chunkCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        doc.setWorkspaceId(WORKSPACE_ID);
        return doc;
    }

    @Test
    void handleDocumentUploaded_shouldProcessFullPipeline() {
        Document document = createDocument(DocumentStatus.UPLOADED);
        DocumentUploadedEvent event = new DocumentUploadedEvent(DOCUMENT_ID, WORKSPACE_ID);
        List<String> chunks = List.of("Chunk 1 content", "Chunk 2 content");
        String parsedContent = "Parsed document content";

        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(documentParserService.parse(any(), eq("text/markdown"))).thenReturn(parsedContent);
        when(documentChunkingService.chunk(parsedContent)).thenReturn(chunks);

        documentEventListener.handleDocumentUploaded(event);

        // Verify pipeline stages
        verify(documentRepository).findById(DOCUMENT_ID);
        verify(documentParserService).parse(any(), eq("text/markdown"));
        verify(documentChunkingService).chunk(parsedContent);
        verify(documentChunkRepository).saveAll(anyList());
        verify(documentEmbeddingService).generateEmbeddings(DOCUMENT_ID, chunks);

        // Verify status transitions
        assertEquals(DocumentStatus.READY, document.getStatus());
        assertEquals(2, document.getChunkCount());

        // Verify 4 saves: PARSING → CHUNKING → EMBEDDING → READY
        verify(documentRepository, times(4)).save(document);
    }

    @Test
    void handleDocumentUploaded_shouldSetFailedOnException() {
        Document document = createDocument(DocumentStatus.UPLOADED);
        DocumentUploadedEvent event = new DocumentUploadedEvent(DOCUMENT_ID, WORKSPACE_ID);

        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(documentParserService.parse(any(), anyString()))
            .thenThrow(new RuntimeException("Failed to read file"));

        documentEventListener.handleDocumentUploaded(event);

        // Should catch exception and set FAILED status
        verify(documentRepository, atLeastOnce()).save(document);
        assertEquals(DocumentStatus.FAILED, document.getStatus());
        assertEquals("Failed to read file", document.getErrorMessage());
    }

    @Test
    void handleDocumentUploaded_shouldHandleFailedWithoutErrorMessage() {
        Document document = createDocument(DocumentStatus.UPLOADED);
        DocumentUploadedEvent event = new DocumentUploadedEvent(DOCUMENT_ID, WORKSPACE_ID);

        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(documentParserService.parse(any(), anyString()))
            .thenThrow(new RuntimeException());

        documentEventListener.handleDocumentUploaded(event);

        assertEquals(DocumentStatus.FAILED, document.getStatus());
        assertEquals("Unknown processing error", document.getErrorMessage());
    }

    @Test
    void handleDocumentUploaded_shouldSkipIfDocumentNotFound() {
        DocumentUploadedEvent event = new DocumentUploadedEvent(DOCUMENT_ID, WORKSPACE_ID);

        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

        documentEventListener.handleDocumentUploaded(event);

        verify(documentRepository, never()).save(any());
        verify(documentParserService, never()).parse(any(), anyString());
        verify(documentChunkingService, never()).chunk(anyString());
        verify(documentEmbeddingService, never()).generateEmbeddings(any(), anyList());
    }

    @Test
    void handleDocumentUploaded_shouldHandleChunkCountZero() {
        Document document = createDocument(DocumentStatus.UPLOADED);
        DocumentUploadedEvent event = new DocumentUploadedEvent(DOCUMENT_ID, WORKSPACE_ID);

        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(documentParserService.parse(any(), eq("text/markdown"))).thenReturn("Parsed content");
        when(documentChunkingService.chunk("Parsed content")).thenReturn(List.of());

        documentEventListener.handleDocumentUploaded(event);

        verify(documentChunkRepository).saveAll(List.of());
        verify(documentEmbeddingService).generateEmbeddings(DOCUMENT_ID, List.of());
        assertEquals(DocumentStatus.READY, document.getStatus());
        assertEquals(0, document.getChunkCount());
    }

    @Test
    void handleDocumentUploaded_shouldHandleDocumentDeletedAfterInitialFetch() {
        DocumentUploadedEvent event = new DocumentUploadedEvent(DOCUMENT_ID, WORKSPACE_ID);

        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.of(createDocument(DocumentStatus.UPLOADED)));
        when(documentParserService.parse(any(), anyString()))
            .thenThrow(new RuntimeException("Parse failure"));
        // Second findById returns null (document deleted concurrently)
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.of(createDocument(DocumentStatus.UPLOADED)))
            .thenReturn(Optional.empty());

        documentEventListener.handleDocumentUploaded(event);

        // Exception caught, but document not found on retry → silently handled
        verify(documentRepository, times(1)).save(any());
    }
}
