package com.shiftleft.hub.document.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
        WITH q AS (
            SELECT plainto_tsquery('english', :query) AS en_query
        )
        SELECT dc.id, dc.document_id, dc.content, dc.chunk_index, d.filename, d.mime_type
        FROM document_chunk dc
        JOIN document d ON d.id = dc.document_id
        CROSS JOIN q
        WHERE dc.tsv_content @@ q.en_query
          AND d.workspace_id = CAST(:workspaceId AS UUID)
        ORDER BY ts_rank(dc.tsv_content, q.en_query) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> ftsSearch(
        @Param("query") String query,
        @Param("workspaceId") UUID workspaceId,
        @Param("limit") int limit);

    @Query(value = """
        SELECT dc.id, dc.document_id, dc.content, dc.chunk_index, d.filename,
               1 - (dc.embedding <=> CAST(:embedding AS vector)) AS cosine_similarity
        FROM document_chunk dc
        JOIN document d ON d.id = dc.document_id
        WHERE d.workspace_id = CAST(:workspaceId AS UUID)
          AND dc.embedding IS NOT NULL
          AND 1 - (dc.embedding <=> CAST(:embedding AS vector)) >= :threshold
        ORDER BY cosine_similarity DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> vectorSearch(
        @Param("embedding") float[] embedding,
        @Param("workspaceId") UUID workspaceId,
        @Param("threshold") double threshold,
        @Param("limit") int limit);
}
