package com.shiftleft.hub.document.api.dto;

import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.domain.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentListResponse(
    UUID id,
    String filename,
    String mimeType,
    DocumentStatus status,
    String errorMessage,
    Long fileSize,
    Integer chunkCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static DocumentListResponse from(Document document) {
        return new DocumentListResponse(
            document.getId(),
            document.getFilename(),
            document.getMimeType(),
            document.getStatus(),
            document.getErrorMessage(),
            document.getFileSize(),
            document.getChunkCount(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }
}
