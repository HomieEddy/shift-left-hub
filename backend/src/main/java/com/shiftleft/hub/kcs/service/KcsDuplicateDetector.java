package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Duplicate detection for KCS drafts via FTS keyword check + pgvector
 * semantic search.
 *
 * <p>Single responsibility: given a {@link TicketResolvedEvent}, return
 * the set of existing article IDs that look similar enough to flag.
 * Both passes are non-blocking — failures degrade to "no semantic
 * check" but the draft still gets created.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KcsDuplicateDetector {

    private static final double DEDUP_SIMILARITY_THRESHOLD = 0.85;
    private static final int DEDUP_TOP_K = 5;

    private final ArticleRepository articleRepository;
    private final VectorStore vectorStore;

    /**
     * Returns the set of article UUIDs whose content is similar to the event
     * (combined FTS keyword + pgvector semantic search). Empty if no candidates.
     *
     * @param event the resolved ticket event
     * @return the set of duplicate article IDs (may be empty)
     */
    public Set<UUID> checkDuplicates(TicketResolvedEvent event) {
        Set<UUID> duplicates = new HashSet<>();
        duplicates.addAll(ftsCandidates(event));
        duplicates.addAll(semanticCandidates(event));
        return duplicates;
    }

    private Set<UUID> ftsCandidates(TicketResolvedEvent event) {
        String searchText = extractKeywords(
            Objects.toString(event.issue(), "") + " " + Objects.toString(event.resolutionNotes(), ""));
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        Page<UUID> ftsResults = articleRepository.searchIdsByText(
            searchText, workspaceId, PageRequest.of(0, DEDUP_TOP_K));

        Set<UUID> ids = new HashSet<>();
        for (UUID id : ftsResults.getContent()) {
            ids.add(id);
        }
        return ids;
    }

    private Set<UUID> semanticCandidates(TicketResolvedEvent event) {
        Set<UUID> ids = new HashSet<>();
        try {
            String queryText = Objects.toString(event.issue(), "") + "\n"
                + Objects.toString(event.resolutionNotes(), "");
            UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
            List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(queryText)
                    .topK(DEDUP_TOP_K)
                    .similarityThreshold(DEDUP_SIMILARITY_THRESHOLD)
                    .filterExpression(new FilterExpressionBuilder().eq("workspace_id", workspaceId.toString()).build())
                    .build());

            for (Document doc : docs) {
                String articleIdStr = (String) doc.getMetadata().get("articleId");
                if (articleIdStr != null) {
                    try {
                        ids.add(UUID.fromString(articleIdStr));
                    } catch (IllegalArgumentException e) {
                        log.warn("Malformed articleId in vector store metadata: {}", articleIdStr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Vector dedup search failed, proceeding without semantic check: {}", e.getMessage());
        }
        return ids;
    }

    private String extractKeywords(String text) {
        return text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
