package com.shiftleft.hub.document.api.dto;

import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.domain.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentUploadResponse(
    UUID id,
    String filename,
    String mimeType,
    DocumentStatus status,
    Long fileSize,
    LocalDateTime createdAt
) {
    public static DocumentUploadResponse from(Document document) {
        return new DocumentUploadResponse(
            document.getId(),
            document.getFilename(),
            document.getMimeType(),
            document.getStatus(),
            document.getFileSize(),
            document.getCreatedAt()
        );
    }
}
