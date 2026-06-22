package com.shiftleft.hub.document.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.category.domain.Category;
import com.shiftleft.hub.category.domain.CategoryRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.common.util.SlugUtils;
import com.shiftleft.hub.document.domain.*;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final DocumentParserService documentParserService;
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

    @Value("${app.document.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Uploads a document file, validates it, checks for duplicates, stores it,
     * and publishes an event to start the async processing pipeline.
     *
     * @param file the uploaded multipart file
     * @param categoryId optional category to assign to this document
     * @return the saved document entity
     */
    public Document uploadDocument(MultipartFile file, UUID categoryId) {
        String mimeType = file.getContentType();

        // Validate MIME type with extension fallback
        String extension = getExtension(file.getOriginalFilename());
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
                throw new DocumentProcessingException(
                    "Unsupported file type. Supported: .md, .txt, .pdf, .html, .htm, .xhtml, .xml, .docx");
            }
        }

        // Resolve generic/unknown MIME types to specific type from extension
        if ((mimeType == null || "application/octet-stream".equals(mimeType)) && !extension.isEmpty()) {
            String resolved = EXTENSION_TO_MIME.get(extension);
            if (resolved != null) {
                mimeType = resolved;
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
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new DocumentProcessingException("Category not found: " + categoryId));
            document.setCategory(category);
        }
        try {
            document = documentRepository.save(document);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("unique") || msg.contains("uq_")) {
                throw new DuplicateDocumentException(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown", contentHash);
            }
            throw new DocumentProcessingException("Data integrity violation during document save", e);
        }

        // Store file on local filesystem
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve(workspaceId.toString())
                .resolve(document.getId().toString());
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
            Path filePath = uploadPath.resolve(safeFilename).normalize();
            // Defense in depth: reject any path that escapes the upload directory
            // (e.g. sanitized filenames that collapse to ".." or absolute paths)
            if (!filePath.startsWith(uploadPath)) {
                throw new DocumentProcessingException(
                    "Invalid filename: path traversal detected");
            }
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
     * Converts a READY document into a knowledge base article.
     * Reads the file content, creates a draft article, and returns its ID.
     *
     * @param documentId the document UUID to convert
     * @param authorEmail the email of the user creating the article
     * @return the created article UUID
     */
    @Transactional
    public UUID convertToArticle(UUID documentId, String authorEmail) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        if (!document.getWorkspaceId().equals(workspaceId)) {
            throw new DocumentNotFoundException(documentId);
        }
        if (document.getStatus() != DocumentStatus.READY) {
            throw new DocumentProcessingException("Document must be in READY status to convert to article");
        }

        // Parse the file to get full text content
        String content;
        try {
            content = documentParserService.parse(
                Paths.get(document.getFilePath()),
                document.getMimeType()
            );
        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to parse document for article conversion", e);
        }

        // Derive title and slug from filename (strip extension)
        String filename = document.getFilename();
        String title = filename != null ? filename.replaceFirst("\\.[^.]+$", "") : "Untitled";
        String slug = SlugUtils.slugify(title);
        if (slug.isEmpty()) {
            slug = "untitled";
        }
        if (articleRepository.findBySlug(slug).isPresent()) {
            slug = SlugUtils.withUniqueSuffix(slug);
        }

        // Look up author
        User author = userRepository.findByEmail(authorEmail)
            .orElseThrow(() -> new DocumentProcessingException("Author not found: " + authorEmail));

        // Create the article
        Article article = Article.builder()
            .titleEn(title)
            .contentEn(content)
            .slug(slug)
            .status(ArticleStatus.DRAFT)
            .author(author)
            .build();
        article.setWorkspaceId(workspaceId);
        article = articleRepository.save(article);

        log.info("Article created from document {} (article id: {})", documentId, article.getId());
        return article.getId();
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
}
