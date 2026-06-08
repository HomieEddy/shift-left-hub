package com.shiftleft.hub.kcs.api;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.article.service.ArticleService;
import com.shiftleft.hub.kcs.api.dto.KcsDraftResponse;
import com.shiftleft.hub.kcs.service.KcsDraftingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Admin REST controller for the KCS draft review queue.
 * <p>Manages the lifecycle of AI-drafted articles from creation
 * through review to approval (PUBLISHED) or rejection (ARCHIVED).
 * All endpoints require ADMIN role — already enforced by the
 * existing {@code /api/admin/**} security pattern.</p>
 */
@RestController
@RequestMapping("/api/admin/kcs/drafts")
@RequiredArgsConstructor
@Slf4j
public class AdminKcsController {

    private final ArticleRepository articleRepository;
    private final ArticleService articleService;
    private final KcsDraftingService kcsDraftingService;

    /**
     * Lists all KCS-drafted articles with pagination.
     * <p>KCS drafts are identified by having a non-null sourceTicketId.</p>
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of draft responses
     */
    @GetMapping
    public Page<KcsDraftResponse> getDrafts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return articleRepository
            .findBySourceTicketIdIsNotNullOrderByCreatedAtDesc(PageRequest.of(page, size))
            .map(kcsDraftingService::enrichDraftResponse);
    }

    /**
     * Gets a single KCS draft by article ID.
     *
     * @param id the article UUID
     * @return the draft response
     */
    @GetMapping("/{id}")
    public KcsDraftResponse getDraftDetail(@PathVariable UUID id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new com.shiftleft.hub.article.domain.ArticleNotFoundException(id));
        if (article.getSourceTicketId() == null) {
            throw new com.shiftleft.hub.article.domain.ArticleNotFoundException(id);
        }
        return kcsDraftingService.enrichDraftResponse(article);
    }

    /**
     * Approves a KCS draft — publishes the article. (D-22)
     * Publishes immediately (same as manual publish).
     *
     * @param id the article UUID
     * @return the published draft response
     */
    @PutMapping("/{id}/approve")
    public KcsDraftResponse approveDraft(@PathVariable UUID id) {
        articleService.publishArticle(id);
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new com.shiftleft.hub.article.domain.ArticleNotFoundException(id));
        log.info("KCS draft {} approved and published", id);
        return kcsDraftingService.enrichDraftResponse(article);
    }

    /**
     * Rejects a KCS draft — archives the article. (D-22)
     *
     * @param id the article UUID
     * @return the archived draft response
     */
    @PutMapping("/{id}/reject")
    public KcsDraftResponse rejectDraft(@PathVariable UUID id) {
        articleService.archiveArticle(id);
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new com.shiftleft.hub.article.domain.ArticleNotFoundException(id));
        log.info("KCS draft {} rejected and archived", id);
        return kcsDraftingService.enrichDraftResponse(article);
    }

    /**
     * Returns the count of pending KCS drafts (DRAFT status).
     * Used by the frontend for the nav badge. (D-19)
     *
     * @return map containing the pending count
     */
    @GetMapping("/pending-count")
    public Map<String, Long> getPendingCount() {
        long count = articleRepository.countBySourceTicketIdIsNotNullAndStatus(ArticleStatus.DRAFT);
        return Map.of("pendingCount", count);
    }

}
