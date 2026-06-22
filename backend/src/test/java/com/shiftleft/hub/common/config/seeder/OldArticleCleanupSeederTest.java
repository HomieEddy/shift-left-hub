package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OldArticleCleanupSeederTest {

    @Mock private ArticleRepository articleRepository;

    private OldArticleCleanupSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new OldArticleCleanupSeeder(articleRepository);
    }

    private Article article(String slug) {
        return Article.builder().id(UUID.randomUUID()).slug(slug).build();
    }

    @Test
    void cleanupOldArticles_noOpWhenNoneExist() {
        // Default: any unmatched findBySlug returns empty Optional
        when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

        seeder.cleanupOldArticles();

        verify(articleRepository, never()).delete(any(Article.class));
    }

    @Test
    void cleanupOldArticles_deletesExistingOldArticles() {
        // Stub a specific slug to return an article; catch-all returns empty
        when(articleRepository.findBySlug("connect-corporate-wifi"))
            .thenReturn(Optional.of(article("connect-corporate-wifi")));
        when(articleRepository.findBySlug(org.mockito.ArgumentMatchers.argThat(
                s -> !"connect-corporate-wifi".equals(s))))
            .thenReturn(Optional.empty());

        seeder.cleanupOldArticles();

        verify(articleRepository).delete(any(Article.class));
    }

    @Test
    void cleanupOldArticles_deletesAllThatExist() {
        when(articleRepository.findBySlug("connect-corporate-wifi"))
            .thenReturn(Optional.of(article("connect-corporate-wifi")));
        when(articleRepository.findBySlug("reset-vpn-password"))
            .thenReturn(Optional.of(article("reset-vpn-password")));
        when(articleRepository.findBySlug("printer-troubleshooting"))
            .thenReturn(Optional.of(article("printer-troubleshooting")));
        when(articleRepository.findBySlug(org.mockito.ArgumentMatchers.argThat(
                s -> !java.util.Set.of("connect-corporate-wifi", "reset-vpn-password", "printer-troubleshooting")
                    .contains(s))))
            .thenReturn(Optional.empty());

        seeder.cleanupOldArticles();

        verify(articleRepository, times(3)).delete(any(Article.class));
    }
}