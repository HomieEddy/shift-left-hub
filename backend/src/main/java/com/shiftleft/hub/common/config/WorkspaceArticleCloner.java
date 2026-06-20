package com.shiftleft.hub.common.config;

import com.shiftleft.hub.ai.service.EmbeddingService;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * After PublicSeeder populates the public workspace, clones articles into each
 * workspace so switching workspaces shows content immediately.
 */
@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class WorkspaceArticleCloner {

    private final WorkspaceRepository workspaceRepository;
    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final EmbeddingService embeddingService;

    /**
     * After PublicSeeder populates the public workspace, clones each article
     * into every non-public workspace with workspace-scoped slugs and tags.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(3)
    public void cloneToWorkspaces() {
        Workspace publicWs = workspaceRepository.findBySlug("public").orElse(null);
        if (publicWs == null) {
            log.debug("Public workspace not found — skipping article cloning");
            return;
        }

        List<Article> publicArticles = articleRepository.findAll().stream()
            .filter(a -> publicWs.getId().equals(a.getWorkspaceId()))
            .filter(a -> a.getStatus() == ArticleStatus.PUBLISHED)
            .toList();

        if (publicArticles.isEmpty()) {
            log.debug("No public articles to clone");
            return;
        }

        List<Workspace> targetWorkspaces = workspaceRepository.findAll().stream()
            .filter(w -> !w.getSlug().equals("public"))
            .toList();

        for (Workspace target : targetWorkspaces) {
            cloneArticles(publicArticles, target);
        }
    }

    private void cloneArticles(List<Article> sourceArticles, Workspace targetWs) {
        UUID targetWsId = targetWs.getId();
        int count = 0;

        for (Article source : sourceArticles) {
            String clonedSlug = targetWs.getSlug() + "-" + source.getSlug();
            if (articleRepository.findBySlug(clonedSlug).isPresent()) {
                continue;
            }

            Article clone = Article.builder()
                .titleEn(source.getTitleEn())
                .titleFr(source.getTitleFr())
                .contentEn(source.getContentEn())
                .contentFr(source.getContentFr())
                .slug(clonedSlug)
                .excerpt(source.getExcerpt())
                .excerptFr(source.getExcerptFr())
                .status(ArticleStatus.PUBLISHED)
                .viewCount(0)
                .publishedAt(LocalDateTime.now())
                .author(source.getAuthor())
                .build();
            clone.setWorkspaceId(targetWsId);

            Set<Tag> workspaceTags = resolveWorkspaceTags(source.getTags(), targetWsId);
            clone.setTags(workspaceTags);

            articleRepository.save(clone);
            embeddingService.generateAndStoreEmbedding(clone);
            count++;
        }

        log.info("Cloned {} articles from Public to workspace '{}' (id={})", count, targetWs.getSlug(), targetWsId);
    }

    private Set<Tag> resolveWorkspaceTags(Set<Tag> sourceTags, UUID targetWsId) {
        Set<Tag> workspaceTags = new HashSet<>();
        for (Tag sourceTag : sourceTags) {
            tagRepository.findByNameEnAndWorkspaceId(sourceTag.getNameEn(), targetWsId)
                .ifPresent(workspaceTags::add);
        }
        if (workspaceTags.isEmpty()) {
            tagRepository.findAll().stream()
                .filter(t -> targetWsId.equals(t.getWorkspaceId()))
                .findFirst()
                .ifPresent(workspaceTags::add);
        }
        return workspaceTags;
    }
}
