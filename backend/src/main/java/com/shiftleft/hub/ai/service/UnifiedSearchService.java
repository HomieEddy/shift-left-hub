package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.document.domain.DocumentChunkRepository;
import com.shiftleft.hub.document.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UnifiedSearchService {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;

    private static final int TOP_K = 10;

    public List<DocumentChunkResult> ftsSearchDocumentChunks(String query, UUID workspaceId) {
        var results = documentChunkRepository.ftsSearch(query, workspaceId, TOP_K);
        return results.stream().map(row -> {
            UUID chunkId = (UUID) row[0];
            UUID documentId = (UUID) row[1];
            String content = (String) row[2];
            Integer chunkIndex = ((Number) row[3]).intValue();
            String filename = (String) row[4];
            String excerpt = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            return new DocumentChunkResult(chunkId, documentId, filename, content, excerpt, chunkIndex, 0);
        }).toList();
    }

    public List<DocumentChunkResult> vectorSearchDocumentChunks(String query, UUID workspaceId, double threshold) {
        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingModel.embed(query);
        } catch (Exception e) {
            log.warn("Failed to generate query embedding for document chunk search: {}", e.getMessage());
            return List.of();
        }

        List<Object[]> results = documentChunkRepository.vectorSearch(queryEmbedding, workspaceId, threshold, TOP_K);
        return results.stream().map(row -> {
            UUID chunkId = (UUID) row[0];
            UUID documentId = (UUID) row[1];
            String content = (String) row[2];
            Integer chunkIndex = ((Number) row[3]).intValue();
            String filename = (String) row[4];
            double score = ((Number) row[5]).doubleValue();
            String excerpt = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            return new DocumentChunkResult(chunkId, documentId, filename, content, excerpt, chunkIndex, score);
        }).toList();
    }

    public record DocumentChunkResult(
        UUID chunkId, UUID documentId, String filename,
        String content, String excerpt, int chunkIndex, double score
    ) {}
}