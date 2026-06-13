package com.shiftleft.hub.common.config;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seeds the Human Resources workspace with 8 tags and 10 bilingual articles
 * sourced from {@code data/seed/kb/hr/*.md} markdown files.
 *
 * <p>Runs at {@code @Order(2)} after {@link MasterSeeder} has created the
 * required users and workspaces. Fully idempotent — safe to run on every
 * startup.
 */
@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class HrSeeder {

    private static final String WORKSPACE_SLUG = "human-resources";
    private static final String CLASS_PATH_PATTERN = "classpath:data/seed/kb/hr/*.md";
    private static final String FR_BODY_SEPARATOR = "\n<!-- FR -->\n";

    private static final List<TagDef> HR_TAGS = List.of(
        new TagDef("Recruitment", "Recrutement", "#2563eb"),
        new TagDef("Benefits", "Avantages sociaux", "#16a34a"),
        new TagDef("Policies", "Politiques", "#9333ea"),
        new TagDef("Onboarding", "Intégration", "#0891b2"),
        new TagDef("Payroll", "Paie", "#ca8a04"),
        new TagDef("Performance", "Performance", "#dc2626"),
        new TagDef("Compliance", "Conformité", "#7c3aed"),
        new TagDef("Training", "Formation", "#ea580c")
    );

    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;

    @Value("${app.kb.seed-enabled:true}")
    private boolean seedEnabled;

    /**
     * Main entry point for HR workspace seeding.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void seed() {
        if (!seedEnabled) {
            log.info("KB seeding is disabled — HR seeder skipped");
            return;
        }

        log.info("HR seeder starting...");

        // Step 1: Find admin user
        User admin = userRepository.findByRole(UserRole.ROLE_ADMIN)
            .stream()
            .findFirst()
            .orElse(null);
        if (admin == null) {
            log.warn("No admin user found — HR seeder skipped");
            return;
        }

        // Step 2: Find HR workspace
        Workspace workspace = workspaceService.findBySlug(WORKSPACE_SLUG).orElse(null);
        if (workspace == null) {
            log.warn("HR workspace (slug: {}) not found — HR seeder skipped", WORKSPACE_SLUG);
            return;
        }
        UUID wsId = workspace.getId();

        // Step 3: Ensure HR tags exist
        Map<String, Tag> tagByNameEn = ensureHrTags(wsId);
        log.info("HR workspace ready — {} tags available", tagByNameEn.size());

        // Step 4: Scan, parse, and create articles from markdown files
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(CLASS_PATH_PATTERN);
            log.info("Found {} HR seed markdown files", resources.length);

            int created = 0;
            int updated = 0;

            for (Resource resource : resources) {
                String content = readFile(resource);
                Map<String, String> frontmatter = parseFrontmatter(content);
                String body = extractBody(content);

                if (frontmatter.isEmpty() || body.isBlank()) {
                    log.warn("Skipping invalid or empty HR seed file: {}", resource.getFilename());
                    continue;
                }

                String slug = frontmatter.get("slug");
                if (slug == null || slug.isBlank()) {
                    log.warn("HR seed file missing slug, skipping: {}", resource.getFilename());
                    continue;
                }

                String titleEn = frontmatter.getOrDefault("title_en", "");
                String titleFr = frontmatter.getOrDefault("title_fr", "");
                String excerpt = frontmatter.getOrDefault("excerpt", "");
                String tagsStr = frontmatter.getOrDefault("tags", "");

                String[] bodyParts = splitBilingualBody(body);
                String contentEn = bodyParts[0];
                String contentFr = bodyParts.length > 1 ? bodyParts[1] : contentEn;

                // Resolve tags from frontmatter
                Set<Tag> resolvedTags = resolveTags(tagByNameEn, tagsStr);

                // Idempotent article creation/update
                Optional<Article> existing = articleRepository.findBySlug(slug);
                Article article;
                if (existing.isPresent()) {
                    article = existing.get();
                    article.setTitleEn(titleEn);
                    article.setTitleFr(titleFr);
                    article.setContentEn(contentEn);
                    article.setContentFr(contentFr);
                    article.setExcerpt(excerpt);
                    article.setStatus(ArticleStatus.PUBLISHED);
                    article.setTags(resolvedTags);
                    updated++;
                    log.debug("Updated HR article: {}", slug);
                } else {
                    article = Article.builder()
                        .titleEn(titleEn)
                        .titleFr(titleFr)
                        .contentEn(contentEn)
                        .contentFr(contentFr)
                        .slug(slug)
                        .excerpt(excerpt)
                        .status(ArticleStatus.PUBLISHED)
                        .viewCount(0)
                        .publishedAt(LocalDateTime.now())
                        .author(admin)
                        .tags(resolvedTags)
                        .build();
                    article.setWorkspaceId(wsId);
                    created++;
                    log.debug("Created HR article: {}", slug);
                }
                articleRepository.save(article);
            }

            log.info("HR seeder completed — created: {}, updated: {}, total: {} articles",
                created, updated, created + updated);

        } catch (Exception e) {
            log.error("Error during HR seeding", e);
        }
    }

    /**
     * Creates HR-specific tags in the given workspace if they do not already exist.
     * Returns a map of English tag name → Tag entity.
     */
    private Map<String, Tag> ensureHrTags(UUID workspaceId) {
        // Fetch all existing tags for this workspace
        List<Tag> existingTags = tagRepository.findAll().stream()
            .filter(t -> workspaceId.equals(t.getWorkspaceId()))
            .toList();

        Map<String, Tag> existingByNameEn = new HashMap<>();
        for (Tag tag : existingTags) {
            existingByNameEn.put(tag.getNameEn(), tag);
        }

        Map<String, Tag> result = new LinkedHashMap<>();

        for (TagDef def : HR_TAGS) {
            Tag tag = existingByNameEn.get(def.nameEn());
            if (tag != null) {
                result.put(def.nameEn(), tag);
            } else {
                Tag newTag = Tag.builder()
                    .nameEn(def.nameEn())
                    .nameFr(def.nameFr())
                    .color(def.color())
                    .build();
                newTag.setWorkspaceId(workspaceId);
                Tag saved = tagRepository.save(newTag);
                result.put(def.nameEn(), saved);
                log.info("Created HR tag: {} (workspace: {})", def.nameEn(), workspaceId);
            }
        }

        return result;
    }

    /**
     * Resolves comma-separated tag names from frontmatter into Tag entities.
     * Unknown tag names are silently ignored.
     */
    private Set<Tag> resolveTags(Map<String, Tag> tagByNameEn, String tagsStr) {
        if (tagsStr == null || tagsStr.isBlank()) {
            return Collections.emptySet();
        }
        Set<Tag> resolved = new HashSet<>();
        for (String name : tagsStr.split(",")) {
            String trimmed = name.trim();
            Tag tag = tagByNameEn.get(trimmed);
            if (tag != null) {
                resolved.add(tag);
            }
        }
        return resolved;
    }

    /**
     * Splits bilingual body content into English and French parts.
     * If no separator is found, both parts contain the same content.
     * If French part is missing, falls back to English.
     */
    private String[] splitBilingualBody(String body) {
        int idx = body.indexOf(FR_BODY_SEPARATOR);
        if (idx < 0) {
            return new String[]{body.trim(), body.trim()};
        }
        String en = body.substring(0, idx).trim();
        String fr = body.substring(idx + FR_BODY_SEPARATOR.length()).trim();
        if (fr.isEmpty()) {
            fr = en;
        }
        return new String[]{en, fr};
    }

    // ========================================================================
    // File reading & frontmatter parsing (reused pattern from KbSeeder)
    // ========================================================================

    /**
     * Reads the full content of a resource file as a UTF-8 string.
     */
    private String readFile(Resource resource) {
        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("Failed to read HR seed file: {} — {}", resource.getFilename(), e.getMessage());
            return "";
        }
    }

    /**
     * Parses YAML-like frontmatter from a markdown file.
     * Expects content between "---" delimiters at the start of the file.
     *
     * @param content the full file content
     * @return a map of frontmatter key-value pairs
     */
    private Map<String, String> parseFrontmatter(String content) {
        Map<String, String> frontmatter = new LinkedHashMap<>();
        if (content == null || !content.startsWith("---")) {
            return frontmatter;
        }
        int endIndex = content.indexOf("---", 3);
        if (endIndex < 0) {
            return frontmatter;
        }
        String block = content.substring(3, endIndex).trim();
        for (String line : block.split("\n")) {
            line = line.trim();
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String key = line.substring(0, colonIdx).trim();
                String value = line.substring(colonIdx + 1).trim();
                frontmatter.put(key, value);
            }
        }
        return frontmatter;
    }

    /**
     * Extracts the body content from a markdown file after the frontmatter.
     *
     * @param content the full file content
     * @return the body content (everything after the second "---")
     */
    private String extractBody(String content) {
        if (content == null || !content.startsWith("---")) {
            return content != null ? content.trim() : "";
        }
        int endIndex = content.indexOf("---", 3);
        if (endIndex < 0) {
            return content.trim();
        }
        String body = content.substring(endIndex + 3).trim();
        return body;
    }

    /**
     * Internal record for tag definition data (English name, French name, color hex).
     */
    private record TagDef(String nameEn, String nameFr, String color) {
    }
}
