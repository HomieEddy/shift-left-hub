package com.shiftleft.hub.common.config;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class KbSeeder {

    private static final String FR_BODY_SEPARATOR = "\n<!-- FR -->\n";

    private static final Set<String> SUPPORTED_TAGS = Set.of(
        "vpn", "password", "security", "networking", "wifi", "email", "mobile",
        "software", "it-requests", "printing", "accounts", "remote-access", "incident", "hardware"
    );

    private static final Map<String, String> TAG_COLORS = Map.ofEntries(
        Map.entry("vpn", "#7c3aed"),
        Map.entry("password", "#dc2626"),
        Map.entry("security", "#dc2626"),
        Map.entry("networking", "#0891b2"),
        Map.entry("wifi", "#0891b2"),
        Map.entry("email", "#2563eb"),
        Map.entry("mobile", "#2563eb"),
        Map.entry("software", "#ca8a04"),
        Map.entry("it-requests", "#ca8a04"),
        Map.entry("printing", "#16a34a"),
        Map.entry("accounts", "#9333ea"),
        Map.entry("remote-access", "#7c3aed"),
        Map.entry("incident", "#dc2626"),
        Map.entry("hardware", "#16a34a")
    );

    private static final Map<String, String> TAG_NAMES_EN = Map.ofEntries(
        Map.entry("vpn", "VPN"),
        Map.entry("password", "Password"),
        Map.entry("security", "Security"),
        Map.entry("networking", "Networking"),
        Map.entry("wifi", "Wi-Fi"),
        Map.entry("email", "Email"),
        Map.entry("mobile", "Mobile"),
        Map.entry("software", "Software"),
        Map.entry("it-requests", "IT Requests"),
        Map.entry("printing", "Printing"),
        Map.entry("accounts", "Accounts"),
        Map.entry("remote-access", "Remote Access"),
        Map.entry("incident", "Incident"),
        Map.entry("hardware", "Hardware")
    );

    private static final Map<String, String> TAG_NAMES_FR = Map.ofEntries(
        Map.entry("vpn", "VPN"),
        Map.entry("password", "Mot de passe"),
        Map.entry("security", "Sécurité"),
        Map.entry("networking", "Réseau"),
        Map.entry("wifi", "Wi-Fi"),
        Map.entry("email", "Courriel"),
        Map.entry("mobile", "Mobile"),
        Map.entry("software", "Logiciel"),
        Map.entry("it-requests", "Demandes IT"),
        Map.entry("printing", "Impression"),
        Map.entry("accounts", "Comptes"),
        Map.entry("remote-access", "Accès distant"),
        Map.entry("incident", "Incident"),
        Map.entry("hardware", "Matériel")
    );

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Value("${app.kb.seed-enabled:true}")
    private boolean seedEnabled;

    private User adminUser;

    /**
     * Seeds KB articles from markdown files after the application is fully initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    @Transactional
    public void seed() {
        if (!seedEnabled) {
            log.info("KB seeding is disabled (app.kb.seed-enabled=false)");
            return;
        }

        adminUser = userRepository.findByRole(UserRole.ROLE_ADMIN).stream()
            .findFirst()
            .orElse(null);

        if (adminUser == null) {
            log.warn("No admin user found — skipping KB seed. Create an admin user first.");
            return;
        }

        ensureTagsExist();

        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:data/seed/kb/*.md");

            int seeded = 0;
            int updated = 0;
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null) {
                    continue;
                }

                var content = readFile(resource);
                var frontmatter = parseFrontmatter(content);
                var bilingualBody = extractBilingualBody(content);
                var contentEn = bilingualBody.get("contentEn");
                var contentFr = bilingualBody.get("contentFr");

                String slug = frontmatter.get("slug");
                if (slug == null || slug.isBlank()) {
                    log.warn("Skipping {} — no slug in frontmatter", fileName);
                    continue;
                }

                var tags = resolveTags(frontmatter.getOrDefault("tags", ""));

                var existing = articleRepository.findBySlug(slug);
                if (existing.isPresent()) {
                    var article = existing.get();
                    article.setTitleEn(frontmatter.get("title_en"));
                    article.setTitleFr(frontmatter.get("title_fr"));
                    article.setContentEn(contentEn);
                    article.setContentFr(contentFr);
                    article.setExcerpt(frontmatter.get("excerpt"));
                    article.setTags(tags);
                    articleRepository.save(article);
                    updated++;
                    log.info("Updated seeded KB article: {}", slug);
                } else {
                    var article = Article.builder()
                        .titleEn(frontmatter.get("title_en"))
                        .titleFr(frontmatter.get("title_fr"))
                        .contentEn(contentEn)
                        .contentFr(contentFr)
                        .slug(slug)
                        .excerpt(frontmatter.get("excerpt"))
                        .status(ArticleStatus.PUBLISHED)
                        .viewCount(0)
                        .publishedAt(LocalDateTime.now())
                        .author(adminUser)
                        .tags(tags)
                        .build();

                    articleRepository.save(article);
                    seeded++;
                    log.info("Seeded KB article: {}", slug);
                }
            }

            if (seeded > 0 || updated > 0) {
                log.info("KB seeding complete — {} inserted, {} updated", seeded, updated);
            } else {
                log.info("KB seeding skipped — no changes detected");
            }
        } catch (Exception e) {
            log.error("Failed to seed KB articles", e);
        }
    }

    private void ensureTagsExist() {
        var existingTags = tagRepository.findAll().stream()
            .collect(Collectors.toMap(Tag::getNameEn, t -> t));

        for (String tagKey : SUPPORTED_TAGS) {
            if (!existingTags.containsKey(TAG_NAMES_EN.get(tagKey))) {
                var tag = Tag.builder()
                    .nameEn(TAG_NAMES_EN.get(tagKey))
                    .nameFr(TAG_NAMES_FR.get(tagKey))
                    .color(TAG_COLORS.get(tagKey))
                    .build();
                tagRepository.save(tag);
                log.info("Created tag: {}", TAG_NAMES_EN.get(tagKey));
            }
        }
    }

    private Set<Tag> resolveTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isBlank()) {
            return Set.of();
        }

        var allTags = tagRepository.findAll().stream()
            .collect(Collectors.toMap(Tag::getNameEn, t -> t));

        var tagNames = List.of(tagsStr.split(",")).stream()
            .map(String::trim)
            .map(TAG_NAMES_EN::get)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());

        return tagNames.stream()
            .map(allTags::get)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private String readFile(Resource resource) throws Exception {
        var sb = new StringBuilder();
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private Map<String, String> parseFrontmatter(String content) {
        var map = new HashMap<String, String>();

        if (!content.startsWith("---\n")) {
            return map;
        }

        int endIndex = content.indexOf("\n---\n", 4);
        if (endIndex == -1) {
            return map;
        }

        var fmBlock = content.substring(4, endIndex);
        for (String line : fmBlock.split("\n")) {
            int colonIndex = line.indexOf(": ");
            if (colonIndex == -1) {
                continue;
            }

            var key = line.substring(0, colonIndex).trim();
            var value = line.substring(colonIndex + 2).trim();

            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
            }
            map.put(key, value);
        }

        return map;
    }

    private String extractBody(String content) {
        if (!content.startsWith("---\n")) {
            return content;
        }

        int endIndex = content.indexOf("\n---\n", 4);
        if (endIndex == -1) {
            return content;
        }

        return content.substring(endIndex + 5).trim();
    }

    private Map<String, String> extractBilingualBody(String content) {
        var body = extractBody(content);
        var map = new HashMap<String, String>();

        int separatorIndex = body.indexOf(FR_BODY_SEPARATOR);
        if (separatorIndex == -1) {
            map.put("contentEn", body);
            map.put("contentFr", body);
            return map;
        }

        var en = body.substring(0, separatorIndex).trim();
        var fr = body.substring(separatorIndex + FR_BODY_SEPARATOR.length()).trim();

        if (fr.isBlank()) {
            fr = en;
        }

        map.put("contentEn", en);
        map.put("contentFr", fr);
        return map;
    }
}
