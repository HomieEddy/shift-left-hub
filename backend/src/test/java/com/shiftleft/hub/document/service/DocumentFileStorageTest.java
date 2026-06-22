package com.shiftleft.hub.document.service;

import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.domain.DocumentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentFileStorageTest {

    @TempDir
    Path tempDir;

    private DocumentFileStorage storage;
    private final UUID workspaceId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        storage = new DocumentFileStorage(tempDir.toString());
    }

    @Test
    void write_shouldPersistFileUnderWorkspaceAndDocumentSubdirs() throws Exception {
        MultipartFile file = new MockMultipartFile(
            "file", "test-doc.md", "text/markdown", "hello world".getBytes());

        Path written = storage.write(file, workspaceId, documentId);

        assertTrue(Files.exists(written));
        assertEquals("test-doc.md", written.getFileName().toString());
        assertTrue(written.startsWith(tempDir.resolve(workspaceId.toString()).resolve(documentId.toString())));
    }

    @Test
    void write_shouldSanitizeUnsafeFilenameChars() throws Exception {
        // Paths.get("...") returns the last segment ("passwd"); the sanitizer then
        // replaces the dot/space/forward-slash chars with underscores. Here we
        // use a filename that survives getFileName() with non-safe chars.
        MultipartFile file = new MockMultipartFile(
            "file", "evil name;rm -rf.md", "text/markdown", "x".getBytes());

        Path written = storage.write(file, workspaceId, documentId);

        assertTrue(Files.exists(written));
        // Space, semicolon, and hyphen all replaced; alphanumeric/underscore/dot kept.
        assertEquals("evil_name_rm_-rf.md", written.getFileName().toString());
        assertTrue(written.startsWith(tempDir.resolve(workspaceId.toString()).resolve(documentId.toString())));
    }

    @Test
    void write_shouldRejectDotDotFilename() {
        // After Paths.get("..").getFileName() returns ".." and the
        // [a-zA-Z0-9_.-] regex keeps dots, a bare ".." filename resolves
        // one level above the document storage directory. The containment
        // check must reject it (S-1).
        MultipartFile file = new MockMultipartFile(
            "file", "..", "text/markdown", "x".getBytes());

        DocumentProcessingException ex = assertThrows(
            DocumentProcessingException.class,
            () -> storage.write(file, workspaceId, documentId));
        assertTrue(ex.getMessage().toLowerCase().contains("path traversal"));
    }

    @Test
    void write_shouldUseDocumentIdAsFallbackWhenOriginalFilenameIsNull() throws Exception {
        // Use a Mockito mock so we can pass null for the original filename
        // (MockMultipartFile's ctor rejects null), and stub the write side-effect
        // so the test stays focused on the safeFilename() fallback contract.
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getBytes()).thenReturn("x".getBytes());

        Path target = storage.write(file, workspaceId, documentId);

        // The returned path should use the documentId as the filename.
        assertEquals(documentId.toString(), target.getFileName().toString());
        assertTrue(target.startsWith(tempDir.resolve(workspaceId.toString()).resolve(documentId.toString())));
    }

    @Test
    void delete_shouldRemoveFileAndParentDir() throws Exception {
        MultipartFile file = new MockMultipartFile(
            "file", "to-delete.md", "text/markdown", "x".getBytes());
        Path written = storage.write(file, workspaceId, documentId);

        Document doc = Document.builder()
            .id(documentId)
            .filePath(written.toString())
            .filename("to-delete.md")
            .build();
        doc.setWorkspaceId(workspaceId);

        storage.delete(doc);

        assertFalse(Files.exists(written));
    }

    @Test
    void delete_shouldBeNoopWhenFilePathIsNull() {
        Document doc = Document.builder()
            .id(documentId)
            .filePath(null)
            .filename("never-uploaded.md")
            .build();
        doc.setWorkspaceId(workspaceId);
        doc.setCreatedAt(LocalDateTime.now());

        // No exception, no side effect.
        assertDoesNotThrow(() -> storage.delete(doc));
    }
}
