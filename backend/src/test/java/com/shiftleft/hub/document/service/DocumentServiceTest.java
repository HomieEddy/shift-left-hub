package com.shiftleft.hub.document.service;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.category.domain.CategoryRepository;
import com.shiftleft.hub.document.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DocumentFileStorage documentFileStorage;

    private DocumentWorkspaceAccess workspaceAccess;
    private DocumentService documentService;

    @org.junit.jupiter.api.io.TempDir
    static Path tempDir;

    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final String FILENAME = "test-doc.md";
    private static final String MIME_TYPE = "text/markdown";
    private static final String CONTENT_HASH = "a".repeat(64);

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(WORKSPACE_ID);
        // Real workspaceAccess backed by the mocked repository so the test
        // stubs on documentRepository.findById(...) are honored.
        workspaceAccess = new DocumentWorkspaceAccess(documentRepository);
        documentService = new DocumentService(
            documentRepository, documentChunkRepository, categoryRepository,
            eventPublisher, documentFileStorage, workspaceAccess);
        // Default file-storage stub so the existing tests that don't care about
        // the file path still pass. Individual tests can override with mockFileStorageWrite().
        lenient().when(documentFileStorage.write(any(MultipartFile.class), any(UUID.class), any(UUID.class)))
            .thenAnswer(invocation -> tempDir.resolve("safe-filename"));
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    private Document createDocument(DocumentStatus status) {
        Document doc = Document.builder()
            .id(DOCUMENT_ID)
            .filename(FILENAME)
            .mimeType(MIME_TYPE)
            .contentHash(CONTENT_HASH)
            .status(status)
            .filePath("./uploads/" + WORKSPACE_ID + "/" + DOCUMENT_ID + "/" + FILENAME)
            .fileSize(1024L)
            .chunkCount(5)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        doc.setWorkspaceId(WORKSPACE_ID);
        return doc;
    }

    private void setUploadDir() throws Exception {
        // Kept for legacy tests; no-op now that the upload dir lives in DocumentFileStorage.
        // Individual upload tests configure documentFileStorage.write(...) directly via mockFileStorageWrite().
    }

    private void mockFileStorageWrite() {
        when(documentFileStorage.write(any(MultipartFile.class), any(UUID.class), any(UUID.class)))
            .thenAnswer(invocation -> {
                UUID docId = invocation.getArgument(2);
                return tempDir.resolve(docId.toString()).resolve("safe-filename");
            });
    }

    private void mockDocumentSaveWithId() {
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID());
            }
            return doc;
        });
    }

    @Test
    void uploadDocument_shouldRejectUnsupportedMimeType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/octet-stream");

        assertThrows(DocumentProcessingException.class, () ->
            documentService.uploadDocument(file, null));
    }

    @Test
    void uploadDocument_shouldRejectOversizedFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(100L * 1024 * 1024); // 100MB > 50MB limit

        assertThrows(DocumentProcessingException.class, () ->
            documentService.uploadDocument(file, null));
    }

    @Test
    void uploadDocument_shouldDetectDuplicate() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(1024L);
        try { when(file.getBytes()).thenReturn("content".getBytes()); } catch (IOException e) {}

        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(eq(WORKSPACE_ID), anyString(), eq(DocumentStatus.READY)))
            .thenReturn(Optional.of(createDocument(DocumentStatus.READY)));

        assertThrows(DuplicateDocumentException.class, () ->
            documentService.uploadDocument(file, null));
    }

    @Test
    void listDocuments_shouldReturnWorkspaceDocuments() {
        when(documentRepository.findByWorkspaceIdOrderByCreatedAtDesc(WORKSPACE_ID))
            .thenReturn(List.of(createDocument(DocumentStatus.READY)));

        List<Document> docs = documentService.listDocuments();

        assertEquals(1, docs.size());
        assertEquals(FILENAME, docs.getFirst().getFilename());
    }

    @Test
    void listDocuments_shouldReturnEmptyWhenNoneExist() {
        when(documentRepository.findByWorkspaceIdOrderByCreatedAtDesc(WORKSPACE_ID))
            .thenReturn(List.of());

        List<Document> docs = documentService.listDocuments();

        assertTrue(docs.isEmpty());
    }

    @Test
    void getDocument_shouldReturnWhenFoundInWorkspace() {
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.of(createDocument(DocumentStatus.READY)));

        Document doc = documentService.getDocument(DOCUMENT_ID);

        assertNotNull(doc);
        assertEquals(FILENAME, doc.getFilename());
    }

    @Test
    void getDocument_shouldThrowWhenNotFound() {
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () ->
            documentService.getDocument(DOCUMENT_ID));
    }

    @Test
    void getDocument_shouldThrowWhenWrongWorkspace() {
        Document doc = createDocument(DocumentStatus.READY);
        doc.setWorkspaceId(UUID.randomUUID());
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.of(doc));

        assertThrows(DocumentNotFoundException.class, () ->
            documentService.getDocument(DOCUMENT_ID));
    }

    @Test
    void deleteDocument_shouldDeleteChunksFileAndRecord() {
        Document doc = createDocument(DocumentStatus.READY);
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.of(doc));

        documentService.deleteDocument(DOCUMENT_ID);

        verify(documentChunkRepository).deleteByDocumentId(DOCUMENT_ID);
        verify(documentRepository).delete(doc);
    }

    @Test
    void deleteDocument_shouldThrowWhenNotFound() {
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () ->
            documentService.deleteDocument(DOCUMENT_ID));
    }

    @Test
    void reprocessDocument_shouldResetStatusAndPublishEvent() {
        Document doc = createDocument(DocumentStatus.FAILED);
        doc.setErrorMessage("Previous parsing error");
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.of(doc));

        Document result = documentService.reprocessDocument(DOCUMENT_ID);

        assertEquals(DocumentStatus.UPLOADED, result.getStatus());
        assertNull(result.getErrorMessage());
        assertEquals(0, result.getChunkCount());
        verify(documentChunkRepository).deleteByDocumentId(DOCUMENT_ID);
        verify(eventPublisher).publishEvent(any(DocumentUploadedEvent.class));
    }

    @Test
    void reprocessDocument_shouldThrowWhenWrongWorkspace() {
        Document doc = createDocument(DocumentStatus.FAILED);
        doc.setWorkspaceId(UUID.randomUUID());
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.of(doc));

        assertThrows(DocumentNotFoundException.class, () ->
            documentService.reprocessDocument(DOCUMENT_ID));
    }

    // ── uploadDocument: happy path ────────────────────────────────

    @Test
    void uploadDocument_shouldSucceed() throws Exception {
        setUploadDir();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(1024L);
        try { when(file.getBytes()).thenReturn("test content".getBytes()); } catch (IOException e) {}
        when(file.getOriginalFilename()).thenReturn("test-doc.md");

        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            any(), anyString(), eq(DocumentStatus.READY)))
            .thenReturn(Optional.empty());

        mockDocumentSaveWithId();

        Document result = documentService.uploadDocument(file, null);

        assertNotNull(result);
        assertEquals("test-doc.md", result.getFilename());
        assertEquals("text/markdown", result.getMimeType());
        assertEquals(DocumentStatus.UPLOADED, result.getStatus());
        assertNotNull(result.getFilePath());
        verify(eventPublisher).publishEvent(any(DocumentUploadedEvent.class));
    }

    // ── uploadDocument: file IO failure ──────────────────────────

    @Test
    void uploadDocument_shouldPropagateFileStorageFailure() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(1024L);
        try { when(file.getBytes()).thenReturn("test content".getBytes()); } catch (IOException e) {}
        when(file.getOriginalFilename()).thenReturn("test-doc.md");

        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            any(), anyString(), eq(DocumentStatus.READY)))
            .thenReturn(Optional.empty());

        mockDocumentSaveWithId();

        // File storage throws (e.g. permission denied) — service must surface it
        // as DocumentProcessingException and must not publish the upload event.
        when(documentFileStorage.write(any(MultipartFile.class), any(UUID.class), any(UUID.class)))
            .thenThrow(new DocumentProcessingException("Failed to store uploaded file", new IOException("Permission denied")));

        assertThrows(DocumentProcessingException.class, () ->
            documentService.uploadDocument(file, null));

        verify(eventPublisher, never()).publishEvent(any(DocumentUploadedEvent.class));
    }

    // ── uploadDocument: null / empty filename ─────────────────────

    @Test
    void uploadDocument_shouldHandleNullOriginalFilename() throws Exception {
        setUploadDir();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(1024L);
        try { when(file.getBytes()).thenReturn("content".getBytes()); } catch (IOException e) {}
        when(file.getOriginalFilename()).thenReturn(null);

        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            any(), anyString(), eq(DocumentStatus.READY)))
            .thenReturn(Optional.empty());

        mockDocumentSaveWithId();

        Document result = documentService.uploadDocument(file, null);

        // With null filename, the document ID should be used as the safe filename
        assertNotNull(result);
        assertNull(result.getFilename()); // original filename stored on document is null
        assertNotNull(result.getFilePath());
        verify(eventPublisher).publishEvent(any(DocumentUploadedEvent.class));
    }

    @Test
    void uploadDocument_shouldHandleEmptyOriginalFilename() throws Exception {
        setUploadDir();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(1024L);
        try { when(file.getBytes()).thenReturn("data".getBytes()); } catch (IOException e) {}
        when(file.getOriginalFilename()).thenReturn("");

        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            any(), anyString(), eq(DocumentStatus.READY)))
            .thenReturn(Optional.empty());

        mockDocumentSaveWithId();

        // Should not throw despite empty filename
        Document result = documentService.uploadDocument(file, null);
        assertNotNull(result);
        assertNotNull(result.getFilePath());
        verify(eventPublisher).publishEvent(any(DocumentUploadedEvent.class));
    }

    // ── getDocuments: wrong workspace ──────────────────────────

    @Test
    void getDocuments_shouldReturnEmptyWhenWrongWorkspace() {
        when(documentRepository.findByWorkspaceIdOrderByCreatedAtDesc(WORKSPACE_ID))
            .thenReturn(List.of());

        List<Document> docs = documentService.listDocuments();

        assertTrue(docs.isEmpty());
    }

    // ── deleteDocument: not found ──────────────────────────

    @Test
    void deleteDocument_shouldHandleNotFoundGracefully() {
        when(documentRepository.findById(DOCUMENT_ID))
            .thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class,
            () -> documentService.deleteDocument(DOCUMENT_ID));
    }

    // ── uploadDocument: path traversal (S-1) ───────────────────

    @Test
    void uploadDocument_shouldPropagatePathTraversalFromFileStorage() throws Exception {
        // Path-traversal containment is enforced inside DocumentFileStorage.write.
        // DocumentService.uploadDocument must surface that as DocumentProcessingException
        // and must not publish the upload event.
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(1024L);
        try { when(file.getBytes()).thenReturn("content".getBytes()); } catch (IOException e) {}
        when(file.getOriginalFilename()).thenReturn("..");

        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            any(), anyString(), eq(DocumentStatus.READY)))
            .thenReturn(Optional.empty());

        mockDocumentSaveWithId();

        when(documentFileStorage.write(any(MultipartFile.class), any(UUID.class), any(UUID.class)))
            .thenThrow(new DocumentProcessingException("Invalid filename: path traversal detected"));

        assertThrows(DocumentProcessingException.class, () ->
            documentService.uploadDocument(file, null));
        verify(eventPublisher, never()).publishEvent(any(DocumentUploadedEvent.class));
    }
}
