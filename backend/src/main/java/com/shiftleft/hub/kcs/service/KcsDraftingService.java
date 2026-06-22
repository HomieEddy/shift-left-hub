package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.util.SlugUtils;
import com.shiftleft.hub.kcs.api.dto.KcsDraftResponse;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.tag.api.dto.TagResponse;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrator for the KCS drafting pipeline.
 *
 * <p>Delegates to single-responsibility collaborators:
 * <ul>
 *   <li>{@link KcsPromptBuilder} — turns the event into an LLM prompt</li>
 *   <li>{@link KcsResponseParser} — turns the LLM text into structured fields</li>
 *   <li>{@link KcsDuplicateDetector} — FTS + semantic check for similar drafts</li>
 *   <li>{@link KcsSimilaritySearch} — keyword search for response enrichment</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class KcsDraftingService {

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final TicketRepository ticketRepository;
    private final KcsPromptBuilder promptBuilder;
    private final KcsResponseParser responseParser;
    private final KcsDuplicateDetector duplicateDetector;
    private final KcsSimilaritySearch similaritySearch;
    private final com.shiftleft.hub.ai.service.AiConfigService aiConfigService;

    /**
     * Synthesizes a bilingual KB article from a resolved ticket and saves it as DRAFT.
     *
     * @param event      the resolved ticket event data
     * @param systemUser a system user to use as the article author
     * @return the created Article entity
     */
    public Article draftArticle(TicketResolvedEvent event, User systemUser) {
        var existing = articleRepository.findBySourceTicketId(event.ticketId());
        if (existing.isPresent()) {
            log.info("KCS draft already exists for ticket {} (article {}), skipping",
                event.ticketNumber(), existing.get().getId());
            return existing.get();
        }

        String prompt = promptBuilder.buildDraftingPrompt(event);
        String llmResponse = callLlm(prompt);
        KcsResponseParser.KcsParsedArticle parsed = responseParser.parse(llmResponse, event);

        String slug = uniqueSlug(parsed.titleEn());
        Set<Tag> tags = resolveSuggestedTags(parsed.suggestedTags());

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

        Set<UUID> duplicateIds = duplicateDetector.checkDuplicates(event);
        if (!duplicateIds.isEmpty()) {
            log.info("KCS draft {} flagged with {} potential duplicate(s): {}",
                article.getId(), duplicateIds.size(), duplicateIds);
        }
        return article;
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
        Set<String> similarityWarnings = similaritySearch.findSimilarArticles(article);
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

    private String callLlm(String prompt) {
        com.shiftleft.hub.ai.domain.AiConfig config = aiConfigService.getConfigEntity();
        org.springframework.ai.chat.client.ChatClient chatClient = aiConfigService.buildChatClient(
            config.getLlmProvider(),
            config.getOllamaEndpointUrl(),
            config.getOpenaiApiKey(),
            config.getChatModelName()
        );

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    private String uniqueSlug(String title) {
        String slug = SlugUtils.slugify(title);
        if (articleRepository.findBySlug(slug).isPresent()) {
            slug = SlugUtils.withUniqueSuffix(slug);
        }
        return slug;
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
        List<Tag> found = tagRepository.findByNameEnIn(
            tagNames.stream().limit(5).collect(Collectors.toList()));
        return new HashSet<>(found);
    }
}
