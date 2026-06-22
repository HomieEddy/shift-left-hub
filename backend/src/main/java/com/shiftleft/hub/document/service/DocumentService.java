package com.shiftleft.hub.document.service;

import com.shiftleft.hub.category.domain.Category;
import com.shiftleft.hub.category.domain.CategoryRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.document.domain.*;
import com.shiftleft.hub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Document service: upload, list, get, reprocess, delete.
 *
 * <p>File system IO is delegated to {@link DocumentFileStorage};
 * document-to-article conversion is delegated to {@link DocumentConverter};
 * workspace-scoped lookup is delegated to {@link DocumentWorkspaceAccess}.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentService {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
        "text/markdown", "text/plain", "application/pdf",
        "text/html", "application/xhtml+xml",
        "text/xml", "application/xml", "application/rss+xml", "application/atom+xml",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".md", ".txt", ".pdf", ".html", ".htm", ".xhtml", ".xml", ".docx"
    );
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB

    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
        Map.entry(".md", "text/markdown"),
        Map.entry(".txt", "text/plain"),
        Map.entry(".pdf", "application/pdf"),
        Map.entry(".html", "text/html"),
        Map.entry(".htm", "text/html"),
        Map.entry(".xhtml", "application/xhtml+xml"),
        Map.entry(".xml", "text/xml"),
        Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    );

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentFileStorage fileStorage;
    private final DocumentWorkspaceAccess workspaceAccess;

    /**
     * Uploads a document file, validates it, checks for duplicates, stores it,
     * and publishes an event to start the async processing pipeline.
     *
     * @param file       the uploaded multipart file
     * @param categoryId optional category to assign to the document
     * @return the saved document entity
     */
    public Document uploadDocument(MultipartFile file, UUID categoryId) {
        String mimeType = resolveMimeType(file);
        validateSize(file);
        String contentHash = sha256(file);

        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        rejectDuplicate(workspaceId, contentHash);

        Document document = createDocumentEntity(file, mimeType, contentHash, workspaceId, categoryId);
        document = saveOrThrowDuplicate(document, file, contentHash);

        Path filePath = fileStorage.write(file, workspaceId, document.getId());
        document.setFilePath(filePath.toString());
        documentRepository.save(document);

        eventPublisher.publishEvent(new DocumentUploadedEvent(document.getId(), workspaceId));
        log.info("Document uploaded: {} (id: {}, workspace: {}, hash: {})",
            document.getFilename(), document.getId(), workspaceId, contentHash);
        return document;
    }

    /**
     * Reprocesses a document by deleting existing chunks, resetting its status,
     * and re-publishing the upload event to restart the ETL pipeline.
     *
     * @param documentId the document UUID to reprocess
     * @return the reset document entity
     */
    @Transactional
    public Document reprocessDocument(UUID documentId) {
        Document document = workspaceAccess.requireInCurrentWorkspace(documentId);

        documentChunkRepository.deleteByDocumentId(documentId);
        document.setStatus(DocumentStatus.UPLOADED);
        document.setErrorMessage(null);
        document.setChunkCount(0);
        documentRepository.save(document);

        UUID workspaceId = document.getWorkspaceId();
        eventPublisher.publishEvent(new DocumentUploadedEvent(documentId, workspaceId));
        log.info("Document reprocess initiated: {} (id: {})", document.getFilename(), documentId);
        return document;
    }

    @Transactional(readOnly = true)
    public List<Document> listDocuments() {
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        return documentRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    /**
     * Returns a single document by ID, scoped to the current workspace.
     *
     * @param documentId the document UUID
     * @return the document entity
     * @throws com.shiftleft.hub.document.domain.DocumentNotFoundException if not found or not in current workspace
     */
    @Transactional(readOnly = true)
    public Document getDocument(UUID documentId) {
        return workspaceAccess.requireInCurrentWorkspace(documentId);
    }

    /**
     * Deletes a document and its associated chunks and file from storage.
     *
     * @param documentId the document UUID to delete
     * @throws com.shiftleft.hub.document.domain.DocumentNotFoundException if not found or not in current workspace
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = workspaceAccess.requireInCurrentWorkspace(documentId);
        documentChunkRepository.deleteByDocumentId(documentId);
        fileStorage.delete(document);
        documentRepository.delete(document);
        log.info("Document deleted: {} (id: {})", document.getFilename(), documentId);
    }

    // ── uploadDocument helpers ──────────────────────────────────────

    private String resolveMimeType(MultipartFile file) {
        String mimeType = file.getContentType();
        String extension = getExtension(file.getOriginalFilename());

        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
                throw new DocumentProcessingException(
                    "Unsupported file type. Supported: .md, .txt, .pdf, .html, .htm, .xhtml, .xml, .docx");
            }
        }
        if ((mimeType == null || "application/octet-stream".equals(mimeType)) && !extension.isEmpty()) {
            String resolved = EXTENSION_TO_MIME.get(extension);
            if (resolved != null) {
                mimeType = resolved;
            }
        }
        return mimeType;
    }

    private void validateSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentProcessingException(
                "File too large: " + file.getSize() + " bytes (max " + MAX_FILE_SIZE + ")");
        }
    }

    private String sha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(file.getBytes());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new DocumentProcessingException("Failed to compute content hash", e);
        }
    }

    private void rejectDuplicate(UUID workspaceId, String contentHash) {
        documentRepository.findByWorkspaceIdAndContentHashAndStatus(workspaceId, contentHash, DocumentStatus.READY)
            .ifPresent(existing -> {
                throw new DuplicateDocumentException(existing.getFilename(), contentHash);
            });
    }

    private Document createDocumentEntity(MultipartFile file, String mimeType, String contentHash,
                                          UUID workspaceId, UUID categoryId) {
        Document document = Document.builder()
            .filename(file.getOriginalFilename())
            .mimeType(mimeType)
            .contentHash(contentHash)
            .status(DocumentStatus.UPLOADED)
            .fileSize(file.getSize())
            .build();
        document.setWorkspaceId(workspaceId);
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new DocumentProcessingException("Category not found: " + categoryId));
            document.setCategory(category);
        }
        return document;
    }

    private Document saveOrThrowDuplicate(Document document, MultipartFile file, String contentHash) {
        try {
            return documentRepository.save(document);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("unique") || msg.contains("uq_")) {
                throw new DuplicateDocumentException(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown", contentHash);
            }
            throw new DocumentProcessingException("Data integrity violation during document save", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return filename.substring(lastDot).toLowerCase();
    }
}
