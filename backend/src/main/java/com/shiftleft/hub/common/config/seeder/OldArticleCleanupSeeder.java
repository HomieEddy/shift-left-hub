package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OldArticleCleanupSeeder {

    private static final List<String> OLD_ARTICLE_SLUGS = List.of(
        "connect-corporate-wifi",
        "software-installation-request",
        "reset-vpn-password",
        "company-email-mobile-setup",
        "report-security-incident",
        "printer-troubleshooting",
        "remote-access-intranet",
        "password-policy-account-security",
        "laptop-docking-station-setup"
    );

    private final ArticleRepository articleRepository;

    /**
     * Deletes any of the OLD_ARTICLE_SLUGS that still exist in the
     * database. No-op if none of them are present.
     */
    public void cleanupOldArticles() {
        boolean anyDeleted = false;
        for (String slug : OLD_ARTICLE_SLUGS) {
            var existing = articleRepository.findBySlug(slug);
            if (existing.isPresent()) {
                articleRepository.delete(existing.get());
                anyDeleted = true;
                log.info("Deleted old seed article: {}", slug);
            }
        }
        if (anyDeleted) {
            log.info("Old seed article cleanup complete - deleted {} articles", OLD_ARTICLE_SLUGS.size());
        } else {
            log.debug("No old seed articles found - cleanup skipped");
        }
    }
}