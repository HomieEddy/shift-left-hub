package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.kcs.api.dto.KcsDraftResponse;
import com.shiftleft.hub.kcs.domain.KcsDraftingException;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.tag.api.dto.TagResponse;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core KCS drafting orchestration service.
 * <p>Handles AI synthesis of KB articles from resolved ticket data,
 * duplicate detection via pgvector semantic search, and article
 * creation with DRAFT status linked to the source ticket.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class KcsDraftingService {

    private final AiConfigService aiConfigService;
    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final TicketRepository ticketRepository;
    private final VectorStore vectorStore;
    private final UserRepository userRepository;

    private static final double DEDUP_SIMILARITY_THRESHOLD = 0.85;
    private static final int DEDUP_TOP_K = 5;

    /**
     * Synthesizes a bilingual KB article from a resolved ticket and saves it as DRAFT.
     *
     * @param event      the resolved ticket event data
     * @param systemUser a system user to use as the article author
     * @return the created Article entity
     * @throws KcsDraftingException on non-retryable errors
     */
    public Article draftArticle(TicketResolvedEvent event, User systemUser) {
        // 0. Check if draft already exists for this source ticket (WR-07)
        java.util.Optional<Article> existing =
            articleRepository.findBySourceTicketId(event.ticketId());
        if (existing.isPresent()) {
            log.info("KCS draft already exists for ticket {} (article {}), skipping",
                event.ticketNumber(), existing.get().getId());
            return existing.get();
        }

        // 1. Build LLM prompt from ticket timeline
        String prompt = buildDraftingPrompt(event);

        // 2. Call LLM to generate bilingual article content
        String llmResponse = callLlm(prompt);

        // 3. Parse LLM response into structured fields
        KcsParsedArticle parsed = parseLlmResponse(llmResponse, event);

        // 4. Generate slug
        String slug = slugify(parsed.titleEn());
        if (articleRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // 5. Resolve suggested tags (find existing tags by name, or skip if none match)
        Set<Tag> tags = resolveSuggestedTags(parsed.suggestedTags());

        // 6. Create and save the article
        Article article = Article.builder()
            .titleEn(parsed.titleEn())
            .contentEn(parsed.contentEn())
            .titleFr(parsed.titleFr())
            .contentFr(parsed.contentFr())
            .slug(slug)
            .excerpt(parsed.excerpt())
            .status(ArticleStatus.DRAFT)
            .viewCount(0)
            .author(systemUser)
            .tags(tags)
            .sourceTicketId(event.ticketId())
            .build();

        article = articleRepository.save(article);
        log.info("KCS draft created: article {} from ticket {}", article.getId(), event.ticketNumber());

        // 7. Check for duplicates (logged for observability)
        Set<UUID> duplicateIds = checkDuplicates(event);
        if (!duplicateIds.isEmpty()) {
            log.info("KCS draft {} flagged with {} potential duplicate(s): {}",
                article.getId(), duplicateIds.size(), duplicateIds);
        }

        return article;
    }

    /**
     * Duplicate detection: lightweight FTS keyword check first, then pgvector semantic search.
     * Returns IDs of published articles with similarity > 0.85 threshold. (D-10, D-12)
     */
    private Set<UUID> checkDuplicates(TicketResolvedEvent event) {
        // FTS fast-path — check if similar articles exist by keyword overlap (D-12)
        String searchText = extractKeywords(
            Objects.toString(event.issue(), "") + " " + Objects.toString(event.resolutionNotes(), ""));
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        var ftsResults = articleRepository.searchByText(searchText, workspaceId,
            org.springframework.data.domain.PageRequest.of(0, 5));

        Set<UUID> duplicates = new HashSet<>();
        if (!ftsResults.isEmpty()) {
            // Use FTS result IDs as preliminary duplicates (IN-05)
            for (Object[] row : ftsResults.getContent()) {
                if (row[0] instanceof UUID id) {
                    duplicates.add(id);
                }
            }
        }

        // Semantic search via pgvector — query with combined text (D-10)
        try {
            String queryText = Objects.toString(event.issue(), "") + "\n"
                + Objects.toString(event.resolutionNotes(), "");
            List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(queryText)
                    .topK(DEDUP_TOP_K)
                    .similarityThreshold(DEDUP_SIMILARITY_THRESHOLD)
                    .filterExpression("workspace_id == '" + workspaceId + "'")
                    .build());

            for (Document doc : docs) {
                String articleIdStr = (String) doc.getMetadata().get("articleId");
                if (articleIdStr != null) {
                    try {
                        duplicates.add(UUID.fromString(articleIdStr));
                    } catch (IllegalArgumentException e) {
                        log.warn("Malformed articleId in vector store metadata: {}", articleIdStr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Vector dedup search failed, proceeding without semantic check: {}", e.getMessage());
            // Non-blocking — proceed with draft creation
        }

        return duplicates;
    }

    /** Builds the LLM prompt with full ticket timeline context. (D-05, D-06, D-07) */
    private String buildDraftingPrompt(TicketResolvedEvent event) {
        return """
You are a Knowledge-Centered Service (KCS) content specialist. \
Create a knowledge base article from a resolved IT support ticket.

## Source Ticket Information
- Ticket Number: %s
- Issue: %s
- Category: %s
- Urgency: %s
- Resolution Notes: %s
- Agent: %s
- User: %s

## Instructions
Create a complete, bilingual knowledge base article in the following format.
Return ONLY the structured fields below — no preamble, no commentary, no markdown code fences.

title_en: <English title, concise and descriptive, max 80 chars>
title_fr: <French translation of the title>
excerpt: <One-sentence summary of the solution in English, max 160 chars>
content_en:
<Full English article content in markdown format. Structure:

## Overview
Brief description of the issue.

## Steps to Resolve
1. Step one
2. Step two
3. Step three

## Notes
Any additional context, prerequisites, or warnings.

>
content_fr:
<Full French translation of the article content, preserving markdown structure.>
suggested_tags: <Comma-separated list of suggested tag names in English>
""".formatted(
                event.ticketNumber(),
                event.issue(),
                event.category(),
                event.urgency(),
                Objects.toString(event.resolutionNotes(), "N/A"),
                event.agentDisplayName(),
                event.userDisplayName()
            );
    }

    /** Calls the LLM using the same provider/config as the chat service. (D-08, D-09) */
    private String callLlm(String prompt) {
        AiConfig config = aiConfigService.getConfigEntity();
        ChatClient chatClient = aiConfigService.buildChatClient(config);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    /** Parses the LLM response into structured fields. */
    private KcsParsedArticle parseLlmResponse(String response, TicketResolvedEvent event) {
        String titleEn = extractField(response, "title_en");
        String excerpt = extractField(response, "excerpt");
        String contentEn = extractField(response, "content_en");

        if (titleEn == null || titleEn.isBlank()) {
            titleEn = event.issue();
        }
        String notes = Objects.toString(event.resolutionNotes(), "");
        if (contentEn == null || contentEn.isBlank()) {
            contentEn = "Resolution: " + notes;
        }
        if (excerpt == null || excerpt.isBlank()) {
            excerpt = notes.length() > 160
                ? notes.substring(0, 157) + "..."
                : notes;
        }

        String tagsStr = extractField(response, "suggested_tags");
        List<String> tags = tagsStr != null && !tagsStr.isBlank()
            ? Arrays.stream(tagsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceAll("[^a-zA-Z0-9\\s-]", ""))
                .toList()
            : List.of();

        String titleFr = extractField(response, "title_fr");
        String contentFr = extractField(response, "content_fr");
        return new KcsParsedArticle(titleEn, titleFr != null ? titleFr : titleEn,
            contentEn, contentFr != null ? contentFr : contentEn,
            excerpt, tags);
    }

    private String extractField(String response, String fieldName) {
        // Normalize line endings to handle cross-platform LLM output (WR-04)
        String normalized = response.replace("\r\n", "\n");
        String prefix = fieldName + ":";
        int start = normalized.indexOf(prefix);
        if (start == -1) {
            return null;
        }
        start += prefix.length();
        // Skip past the newline after the field name prefix to get content start
        if (start < normalized.length() && normalized.charAt(start) == '\n') {
            start++;
        }

        // Find the next field marker — search for "\n" + marker at line start
        int end = normalized.length();
        String[] markers = {"title_en:", "title_fr:", "excerpt:", "content_en:", "content_fr:", "suggested_tags:"};
        for (String marker : markers) {
            if (marker.equals(fieldName + ":")) {
                continue;
            }
            int idx = normalized.indexOf("\n" + marker, start);
            if (idx != -1 && idx < end) {
                end = idx;
            }
        }

        String value = normalized.substring(start, end).trim();
        // Remove markdown code fences if present
        value = value.replaceAll("```\\w*", "").trim();
        return value;
    }

    /** Extracts search keywords from text for FTS dedup check. */
    private String extractKeywords(String text) {
        return text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String slugify(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }

    /** Resolves tag names to existing Tag entities — only matches exact name_en. */
    private Set<Tag> resolveSuggestedTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        int total = tagNames.size();
        if (total > 5) {
            log.warn("Tag suggestions truncated to 5 — {} tags suggested", total);
        }
        List<Tag> found = tagRepository.findByNameEnIn(tagNames.stream().limit(5).collect(Collectors.toList()));
        return new HashSet<>(found);
    }

    /**
     * Enriches a KcsDraftResponse with the source ticket number and similarity warnings.
     *
     * @param article the KCS draft article
     * @return the enriched draft response
     */
    public KcsDraftResponse enrichDraftResponse(Article article) {
        String ticketNumber = article.getSourceTicketId() != null
            ? ticketRepository.findById(article.getSourceTicketId())
                .map(t -> t.getTicketNumber())
                .orElse(null)
            : null;
        Set<String> similarityWarnings = findSimilarArticles(article);
        return new KcsDraftResponse(
            article.getId(),
            article.getTitleEn(),
            article.getTitleFr(),
            article.getSlug(),
            article.getExcerpt(),
            article.getStatus(),
            article.getSourceTicketId(),
            ticketNumber,
            similarityWarnings,
            article.getTags().stream()
                .map(TagResponse::from)
                .collect(Collectors.toSet()),
            article.getCreatedAt()
        );
    }

    /**
     * Finds potentially duplicate articles by checking title keyword overlap.
     */
    private Set<String> findSimilarArticles(Article article) {
        if (article.getTitleEn() == null || article.getTitleEn().isBlank()) {
            return Set.of();
        }
        String keywords = article.getTitleEn().toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (keywords.isEmpty()) {
            return Set.of();
        }
        try {
            UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
            var results = articleRepository.searchByText(keywords, workspaceId, PageRequest.of(0, 5));
            return results.getContent().stream()
                .map(row -> (Object[]) row)
                .filter(row -> {
                    UUID id = (UUID) row[0];
                    return !id.equals(article.getId());
                })
                .map(row -> (String) row[1])
                .filter(Objects::nonNull)
                .limit(3)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Similar article check failed: {}", e.getMessage());
            return Set.of();
        }
    }

    /** Internal record for parsed LLM output. */
    private record KcsParsedArticle(
        String titleEn, String titleFr,
        String contentEn, String contentFr,
        String excerpt, List<String> suggestedTags
    ) {
    }
}
