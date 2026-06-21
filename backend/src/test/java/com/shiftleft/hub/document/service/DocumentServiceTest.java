package com.shiftleft.hub.document.service;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
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
    @Mock private ApplicationEventPublisher eventPublisher;


    @InjectMocks private DocumentService documentService;

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
        var field = DocumentService.class.getDeclaredField("uploadDir");
        field.setAccessible(true);
        field.set(documentService, tempDir.toString());
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
    void uploadDocument_shouldHandleFileIoFailure() throws Exception {
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

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            filesMock.when(() -> java.nio.file.Files.createDirectories(any(java.nio.file.Path.class)))
                .thenThrow(new java.io.IOException("Permission denied"));

            assertThrows(DocumentProcessingException.class, () ->
                documentService.uploadDocument(file, null));
        }

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

    // ── findByContentHash ─────────────────────────────────────────

    @Test
    void findByContentHash_shouldReturnDocument() {
        Document doc = createDocument(DocumentStatus.READY);
        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            WORKSPACE_ID, CONTENT_HASH, DocumentStatus.READY))
            .thenReturn(Optional.of(doc));

        Document result = documentService.findByContentHash(WORKSPACE_ID, CONTENT_HASH);

        assertNotNull(result);
        assertEquals(CONTENT_HASH, result.getContentHash());
    }

    @Test
    void findByContentHash_shouldReturnNullWhenNotFound() {
        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            WORKSPACE_ID, CONTENT_HASH, DocumentStatus.READY))
            .thenReturn(Optional.empty());

        Document result = documentService.findByContentHash(WORKSPACE_ID, CONTENT_HASH);

        assertNull(result);
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
    void uploadDocument_shouldRejectDotDotFilename() throws Exception {
        // After Paths.get("..").getFileName() returns ".." and the
        // [a-zA-Z0-9_.-] regex keeps dots, a bare ".." filename resolves
        // one level above the document storage directory. The containment
        // check in uploadDocument must reject it.
        setUploadDir();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/markdown");
        when(file.getSize()).thenReturn(1024L);
        try { when(file.getBytes()).thenReturn("content".getBytes()); } catch (IOException e) {}
        when(file.getOriginalFilename()).thenReturn("..");

        when(documentRepository.findByWorkspaceIdAndContentHashAndStatus(
            any(), anyString(), eq(DocumentStatus.READY)))
            .thenReturn(Optional.empty());

        mockDocumentSaveWithId();

        assertThrows(DocumentProcessingException.class, () ->
            documentService.uploadDocument(file, null));
        verify(eventPublisher, never()).publishEvent(any(DocumentUploadedEvent.class));
    }
}
