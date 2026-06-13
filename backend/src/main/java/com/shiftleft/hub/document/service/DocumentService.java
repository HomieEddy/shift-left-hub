package com.shiftleft.hub.document.service;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.document.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ApplicationEventPublisher eventPublisher;

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

    @Value("${app.document.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Uploads a document file, validates it, checks for duplicates, stores it,
     * and publishes an event to start the async processing pipeline.
     *
     * @param file the uploaded multipart file
     * @return the saved document entity
     */
    public Document uploadDocument(MultipartFile file) {
        String mimeType = file.getContentType();

        // Validate MIME type with extension fallback
        String extension = getExtension(file.getOriginalFilename());
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
                throw new DocumentProcessingException(
                    "Unsupported file type. Supported: .md, .txt, .pdf, .html, .htm, .xhtml, .xml, .docx");
            }
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentProcessingException(
                "File too large: " + file.getSize() + " bytes (max " + MAX_FILE_SIZE + ")");
        }

        // Compute SHA-256 content hash
        String contentHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(file.getBytes());
            contentHash = HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new DocumentProcessingException("Failed to compute content hash", e);
        }

        // Check for duplicate (same content hash in same workspace with READY status)
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        documentRepository.findByWorkspaceIdAndContentHashAndStatus(workspaceId, contentHash, DocumentStatus.READY)
            .ifPresent(existing -> {
                throw new DuplicateDocumentException(existing.getFilename(), contentHash);
            });

        // Create document entity
        Document document = Document.builder()
            .filename(file.getOriginalFilename())
            .mimeType(mimeType)
            .contentHash(contentHash)
            .status(DocumentStatus.UPLOADED)
            .fileSize(file.getSize())
            .build();
        document.setWorkspaceId(workspaceId);
        try {
            document = documentRepository.save(document);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateDocumentException(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown", contentHash);
        }

        // Store file on local filesystem
        try {
            Path uploadPath = Paths.get(uploadDir, workspaceId.toString(), document.getId().toString());
            Files.createDirectories(uploadPath);
            // Sanitize filename to prevent path traversal
            String safeFilename = file.getOriginalFilename();
            if (safeFilename != null) {
                Path fileNamePath = Paths.get(safeFilename).getFileName();
                safeFilename = fileNamePath != null ? fileNamePath.toString() : safeFilename;
                safeFilename = safeFilename.replaceAll("[^a-zA-Z0-9_.-]", "_");  // sanitize chars
            } else {
                safeFilename = document.getId().toString();
            }
            Path filePath = uploadPath.resolve(safeFilename);
            file.transferTo(filePath.toFile());
            document.setFilePath(filePath.toString());
            documentRepository.save(document);
        } catch (IOException e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage("Failed to store file: " + e.getMessage());
            documentRepository.save(document);
            throw new DocumentProcessingException("Failed to store uploaded file", e);
        }

        // Publish event to start async pipeline
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
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        if (!document.getWorkspaceId().equals(workspaceId)) {
            throw new DocumentNotFoundException(documentId);
        }

        // Delete old chunks
        documentChunkRepository.deleteByDocumentId(documentId);

        // Reset status and publish event
        document.setStatus(DocumentStatus.UPLOADED);
        document.setErrorMessage(null);
        document.setChunkCount(0);
        documentRepository.save(document);

        eventPublisher.publishEvent(new DocumentUploadedEvent(documentId, workspaceId));
        log.info("Document reprocess initiated: {} (id: {})", document.getFilename(), documentId);
        return document;
    }

    /**
     * Returns all documents for the current workspace, ordered by creation date descending.
     *
     * @return list of documents in the workspace
     */
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
     * @throws DocumentNotFoundException if not found or not in current workspace
     */
    @Transactional(readOnly = true)
    public Document getDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        if (!document.getWorkspaceId().equals(workspaceId)) {
            throw new DocumentNotFoundException(documentId);
        }
        return document;
    }

    /**
     * Deletes a document and its associated chunks and file from storage.
     *
     * @param documentId the document UUID to delete
     * @throws DocumentNotFoundException if not found or not in current workspace
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        if (!document.getWorkspaceId().equals(workspaceId)) {
            throw new DocumentNotFoundException(documentId);
        }

        // Delete chunks first
        documentChunkRepository.deleteByDocumentId(documentId);
        // Delete from filesystem
        try {
            String filePathStr = document.getFilePath();
            if (filePathStr != null) {
                Path filePath = Paths.get(filePathStr);
                Files.deleteIfExists(filePath);
                Path parentPath = filePath.getParent();
                if (parentPath != null) {
                    Files.deleteIfExists(parentPath);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to delete file for document {}: {}", documentId, e.getMessage());
        }
        // Delete document record
        documentRepository.delete(document);
        log.info("Document deleted: {} (id: {})", document.getFilename(), documentId);
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

    /**
     * Finds a READY document by workspace and content hash.
     *
     * @param workspaceId the workspace UUID
     * @param contentHash the SHA-256 content hash
     * @return the document entity, or null if not found
     */
    @Transactional(readOnly = true)
    public Document findByContentHash(UUID workspaceId, String contentHash) {
        return documentRepository.findByWorkspaceIdAndContentHashAndStatus(
                workspaceId, contentHash, DocumentStatus.READY)
            .orElse(null);
    }
}
