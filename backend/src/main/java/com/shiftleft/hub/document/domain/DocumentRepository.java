package com.shiftleft.hub.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Document entities.
 * Provides workspace-scoped query methods for document management operations.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    Optional<Document> findByWorkspaceIdAndContentHashAndStatus(
        UUID workspaceId, String contentHash, DocumentStatus status);

    List<Document> findByWorkspaceIdAndStatus(UUID workspaceId, DocumentStatus status);

    long countByWorkspaceIdAndStatus(UUID workspaceId, DocumentStatus status);

    List<Document> findByCategoryId(UUID categoryId);

    long countByCategoryId(UUID categoryId);

    @Modifying
    @Query("UPDATE Document d SET d.category.id = :categoryId WHERE d.category.id = :sourceId")
    int reassignCategory(@Param("sourceId") UUID sourceId, @Param("categoryId") UUID categoryId);
}
