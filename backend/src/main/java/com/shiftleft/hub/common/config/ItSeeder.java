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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * IT workspace seeder — creates IT tags and bilingual articles from markdown files.
 *
 * <p>Scans {@code classpath:data/seed/kb/it/*.md} for bilingual markdown files,
 * creates the 12 IT workspace tags, and creates 10 published articles
 * authored by the admin user in the IT workspace.
 *
 * <p>Fully idempotent — safe to run on every startup.
 */
@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class ItSeeder {

    private static final String WORKSPACE_SLUG = "it";
    private static final String MARKDOWN_PATTERN = "classpath:data/seed/kb/it/*.md";
    private static final String FR_BODY_SEPARATOR = "\n<!-- FR -->\n";

    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Value("${app.kb.seed-enabled:true}")
    private boolean seedEnabled;

    /**
     * Main entry point for IT seeding.
     *
     * <p>Scans markdown files, creates IT tags and articles in the IT workspace.
     * Fully idempotent — checks by slug for articles and by name_en+workspaceId for tags.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void seed() {
        if (!seedEnabled) {
            log.info("IT seeding skipped — seed-enabled is false");
            return;
        }

        // Find admin user
        User admin = userRepository.findByRole(UserRole.ROLE_ADMIN).stream()
            .findFirst()
            .orElse(null);
        if (admin == null) {
            log.warn("IT seeding skipped — no admin user found");
            return;
        }

        // Find IT workspace
        Workspace itWs = workspaceService.findBySlug(WORKSPACE_SLUG).orElse(null);
        if (itWs == null) {
            log.warn("IT seeding skipped — workspace '{}' not found", WORKSPACE_SLUG);
            return;
        }
        UUID itWsId = itWs.getId();

        // Step 1: Ensure all IT tags exist
        Map<String, Tag> tagByNameEn = ensureTags(itWsId);
        log.info("IT workspace: {} tags ready", tagByNameEn.size());

        // Step 2: Scan and process markdown files
        int created = 0;
        int updated = 0;
        try {
            Resource[] resources = resolver.getResources(MARKDOWN_PATTERN);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }

                String content = readFile(resource);
                Map<String, String> frontmatter = parseFrontmatter(content);
                Map<String, String> bodyParts = extractBilingualBody(content);

                String slug = frontmatter.get("slug");
                if (slug == null || slug.isBlank()) {
                    log.warn("Skipping {} — no slug in frontmatter", filename);
                    continue;
                }

                String titleEn = frontmatter.get("title_en");
                String titleFr = frontmatter.get("title_fr");
                String excerpt = frontmatter.get("excerpt");
                String excerptFr = frontmatter.getOrDefault("excerpt_fr", "");
                String contentEn = bodyParts.get("en");
                String contentFr = bodyParts.get("fr");
                String tagsStr = frontmatter.get("tags");

                // Resolve tag references
                Set<Tag> articleTags = resolveTags(tagsStr, tagByNameEn);

                // Check idempotency by slug
                var existing = articleRepository.findBySlug(slug);
                if (existing.isPresent()) {
                    Article article = existing.get();
                    article.setTitleEn(titleEn);
                    article.setTitleFr(titleFr);
                    article.setContentEn(contentEn);
                    article.setContentFr(contentFr);
                    article.setExcerpt(excerpt);
                    article.setExcerptFr(excerptFr);
                    article.setTags(articleTags);
                    article.setAuthor(admin);
                    article.setWorkspaceId(itWsId);
                    article.setStatus(ArticleStatus.PUBLISHED);
                    article.setPublishedAt(LocalDateTime.now());
                    articleRepository.save(article);
                    updated++;
                    log.debug("Updated article: {}", slug);
                } else {
                    Article article = Article.builder()
                        .titleEn(titleEn)
                        .titleFr(titleFr)
                        .contentEn(contentEn)
                        .contentFr(contentFr)
                        .slug(slug)
                        .excerpt(excerpt)
                        .excerptFr(excerptFr)
                        .status(ArticleStatus.PUBLISHED)
                        .viewCount(0)
                        .publishedAt(LocalDateTime.now())
                        .author(admin)
                        .tags(articleTags)
                        .build();
                    article.setWorkspaceId(itWsId);
                    articleRepository.save(article);
                    created++;
                    log.debug("Created article: {}", slug);
                }
            }
        } catch (Exception e) {
            log.error("Error during IT seeding", e);
            return;
        }

        log.info("IT seeding complete — created: {}, updated: {}", created, updated);
    }

    // =========================================================================
    // Tag creation
    // =========================================================================

    private Map<String, Tag> ensureTags(UUID workspaceId) {
        // Define the 12 IT tags (preserving existing color palette from old KbSeeder)
        List<TagSeed> tagSeeds = List.of(
            new TagSeed("VPN", "VPN", "#7c3aed"),
            new TagSeed("Password", "Mot de passe", "#dc2626"),
            new TagSeed("Security", "Sécurité", "#dc2626"),
            new TagSeed("Networking", "Réseau", "#0891b2"),
            new TagSeed("Wi-Fi", "Wi-Fi", "#0891b2"),
            new TagSeed("Email", "Courriel", "#2563eb"),
            new TagSeed("Software", "Logiciel", "#ca8a04"),
            new TagSeed("Printing", "Impression", "#16a34a"),
            new TagSeed("Accounts", "Comptes", "#9333ea"),
            new TagSeed("Remote Access", "Accès distant", "#7c3aed"),
            new TagSeed("Incident", "Incident", "#dc2626"),
            new TagSeed("Hardware", "Matériel", "#16a34a")
        );

        // Fetch all existing tags for the workspace
        List<Tag> existingTags = tagRepository.findAll().stream()
            .filter(t -> workspaceId.equals(t.getWorkspaceId()))
            .toList();
        Set<String> existingNameEn = existingTags.stream()
            .map(Tag::getNameEn)
            .collect(Collectors.toSet());

        Map<String, Tag> tagByNameEn = new HashMap<>();
        for (Tag tag : existingTags) {
            tagByNameEn.put(tag.getNameEn(), tag);
        }

        // Create any missing tags
        for (TagSeed seed : tagSeeds) {
            if (!existingNameEn.contains(seed.nameEn())) {
                Tag tag = Tag.builder()
                    .nameEn(seed.nameEn())
                    .nameFr(seed.nameFr())
                    .color(seed.color())
                    .build();
                tag.setWorkspaceId(workspaceId);
                tag = tagRepository.save(tag);
                tagByNameEn.put(tag.getNameEn(), tag);
                log.info("Created IT tag: {} ({})", seed.nameEn(), seed.color());
            } else {
                log.debug("IT tag {} already exists — skipping", seed.nameEn());
            }
        }

        return tagByNameEn;
    }

    // =========================================================================
    // File reading utilities
    // =========================================================================

    private String readFile(Resource resource) throws Exception {
        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private Map<String, String> parseFrontmatter(String content) {
        Map<String, String> frontmatter = new HashMap<>();
        if (!content.startsWith("---")) {
            return frontmatter;
        }
        int endIndex = content.indexOf("---", 3);
        if (endIndex == -1) {
            return frontmatter;
        }
        String fmBlock = content.substring(3, endIndex).trim();
        for (String line : fmBlock.split("\n")) {
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String key = line.substring(0, colonIdx).trim();
                String value = line.substring(colonIdx + 1).trim();
                frontmatter.put(key, value);
            }
        }
        return frontmatter;
    }

    private Map<String, String> extractBilingualBody(String content) {
        Map<String, String> parts = new HashMap<>();
        // Remove frontmatter
        String body;
        int endFm = content.indexOf("---", 3);
        if (content.startsWith("---") && endFm != -1) {
            body = content.substring(endFm + 3).trim();
        } else {
            body = content.trim();
        }

        int separatorIdx = body.indexOf(FR_BODY_SEPARATOR);
        if (separatorIdx != -1) {
            parts.put("en", body.substring(0, separatorIdx).trim());
            parts.put("fr", body.substring(separatorIdx + FR_BODY_SEPARATOR.length()).trim());
        } else {
            // No separator — entire body is English
            parts.put("en", body);
            parts.put("fr", body);
        }
        return parts;
    }

    private Set<Tag> resolveTags(String tagsStr, Map<String, Tag> tagByNameEn) {
        if (tagsStr == null || tagsStr.isBlank()) {
            return new HashSet<>();
        }
        Set<Tag> tags = new HashSet<>();
        for (String tagName : tagsStr.split(",")) {
            String trimmed = tagName.trim();
            Tag tag = tagByNameEn.get(trimmed);
            if (tag != null) {
                tags.add(tag);
            } else {
                log.warn("Tag '{}' not found in IT workspace tags", trimmed);
            }
        }
        return tags;
    }

    // =========================================================================
    // Internal records
    // =========================================================================

    private record TagSeed(String nameEn, String nameFr, String color) {
    }
}
