package com.shiftleft.hub.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DocumentChunk entities.
 * Provides document-scoped query methods for chunk operations.
 */
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    long countByDocumentId(UUID documentId);
}
