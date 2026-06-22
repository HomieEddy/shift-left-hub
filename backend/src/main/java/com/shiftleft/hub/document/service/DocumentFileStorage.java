package com.shiftleft.hub.document.service;

import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.domain.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Owns file-system persistence of uploaded documents.
 *
 * <p>Single responsibility: write a MultipartFile to a workspace-scoped path
 * with safe filenames, and remove the file when the document is deleted.
 * Path traversal is rejected by a containment check on the resolved path
 * (defense in depth on top of the filename sanitization — see S-1).
 */
@Component
@Slf4j
public class DocumentFileStorage {

    private final String uploadDir;

    public DocumentFileStorage(@Value("${app.document.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * Writes the upload to disk under {@code {uploadDir}/{workspaceId}/{documentId}/}
     * with a sanitized filename. Returns the absolute path written.
     *
     * @param file        the uploaded file to persist
     * @param workspaceId the workspace scope for the storage path
     * @param documentId  the document UUID (used as the storage subdirectory and fallback filename)
     * @return the absolute path of the written file
     * @throws DocumentProcessingException on any IO failure or path-traversal attempt
     */
    public Path write(MultipartFile file, UUID workspaceId, UUID documentId) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve(workspaceId.toString())
                .resolve(documentId.toString());
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(safeFilename(file, documentId)).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new DocumentProcessingException("Invalid filename: path traversal detected");
            }
            file.transferTo(filePath.toFile());
            return filePath;
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to store uploaded file", e);
        }
    }

    /**
     * Deletes the file for the given document, if a file path is recorded.
     * Best-effort: logs and continues on IO failure so the DB delete is not blocked.
     *
     * @param document the document whose file should be removed
     */
    public void delete(Document document) {
        String filePathStr = document.getFilePath();
        if (filePathStr == null) {
            return;
        }
        try {
            Path filePath = Paths.get(filePathStr);
            Files.deleteIfExists(filePath);
            Path parentPath = filePath.getParent();
            if (parentPath != null) {
                Files.deleteIfExists(parentPath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file for document {}: {}",
                document.getId(), e.getMessage());
        }
    }

    private String safeFilename(MultipartFile file, UUID documentId) {
        String original = file.getOriginalFilename();
        if (original == null) {
            return documentId.toString();
        }
        Path fileNamePath = Paths.get(original).getFileName();
        String safe = fileNamePath != null ? fileNamePath.toString() : original;
        return safe.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
